package top.likoslupus.cellulosesz.core.command.registry

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.core.command.CommandCatalog
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
import top.likoslupus.cellulosesz.core.command.CommandInterceptor
import top.likoslupus.cellulosesz.core.command.dsl.BrigadierCompiler
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import java.util.concurrent.atomic.AtomicInteger

/**
 * Builds one fresh Brigadier root for a single literal. Injected so registry decision logic can be
 * exercised with pure Brigadier in unit tests; production always uses [BrigadierCompiler].
 */
internal fun interface CommandRootBuilder {

    fun build(
        definition: CommandDefinition,
        literal: String,
        permissions: PermissionService,
        buildContext: CommandBuildContext,
        additionalPermission: PermissionSpec?,
        interceptor: CommandInterceptor?,
    ): LiteralCommandNode<CommandSourceStack>

}

/**
 * The complete set of CellulosesZ command declarations. It validates cross-definition uniqueness
 * (literal, alias, permission node) up front, then plans and performs registration against the live
 * dispatcher one route at a time.
 *
 * Ownership is never assumed from a declared name: an existing root is inspected *before* any write,
 * and a conflicting short name is skipped rather than silently merged. Every pass returns a
 * [CommandRegistrationReport] keyed to a fresh generation; no node is retained between dispatchers.
 */
public class CommandRegistry(
    public val definitions: List<CommandDefinition>,
) {

    internal var rootBuilder: CommandRootBuilder = CommandRootBuilder(
        BrigadierCompiler::buildRoot
    )

    internal var treeMutator: CommandTreeMutator = ReflectiveCommandTreeMutator

    internal constructor(
        definitions: List<CommandDefinition>,
        rootBuilder: CommandRootBuilder,
        treeMutator: CommandTreeMutator = ReflectiveCommandTreeMutator,
    ) : this(definitions) {
        this.rootBuilder = rootBuilder
        this.treeMutator = treeMutator
    }

    private val generationCounter = AtomicInteger(0)

    /** Nodes installed for the current dispatcher generation, so a repeat pass is idempotent. */
    private var lastDispatcher: CommandDispatcher<CommandSourceStack>? = null
    private var lastInstalled: Map<String, CommandNode<CommandSourceStack>> = emptyMap()

    init {
        val problems = CommandCatalog.validate(definitions.map(CommandDefinition::toDescriptor))
        require(problems.isEmpty()) {
            "invalid command registry: ${problems.joinToString("; ")}"
        }
    }

    public fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        buildContext: CommandBuildContext,
        permissions: PermissionService,
        policy: CommandRoutingPolicy = CommandRoutingPolicy(),
        interceptor: CommandInterceptor? = null,
    ): CommandRegistrationReport {
        val generation = generationCounter.incrementAndGet()
        if (dispatcher !== lastDispatcher) {
            lastDispatcher = dispatcher
            lastInstalled = emptyMap()
        }
        val plan = CommandRoutePlanner.plan(definitions, policy)

        val installed = mutableMapOf<String, CommandNode<CommandSourceStack>>()
        val results = plan.map { route ->
            attempt(
                dispatcher,
                buildContext,
                permissions,
                generation,
                route,
                installed,
                interceptor,
            )
        }
        lastInstalled = installed
        return CommandRegistrationReport(generation, results)
    }

    private fun attempt(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        buildContext: CommandBuildContext,
        permissions: PermissionService,
        generation: Int,
        route: CommandRoute,
        installedInto: MutableMap<String, CommandNode<CommandSourceStack>>,
        interceptor: CommandInterceptor?,
    ): CommandRouteResult {
        val fallback = fallbackFor(route)

        // A node we installed earlier for this same dispatcher is not a conflict: re-registering the
        // same generation (for example an event that fires twice) is a no-op.
        val alreadyInstalled = installedInto[route.literal] ?: lastInstalled[route.literal]
        if (alreadyInstalled != null) {
            installedInto[route.literal] = alreadyInstalled
            return CommandRouteResult(
                route.literal, route.kind, route.definition.name,
                CommandRouteStatus.INSTALLED, fallback,
            )
        }

        val existing: CommandNode<CommandSourceStack>? = dispatcher.root.getChild(route.literal)
        var vanillaAlias: String? = null
        if (existing != null) {
            if (route.kind != CommandRouteKind.VANILLA_OVERRIDE) {
                val status = statusForOccupiedRoute(route.kind)
                if (route.kind.requiredRoute()) {
                    LOGGER.error(
                        "command_route_conflict literal={} route={} reason=already_present fallback={} generation={}",
                        route.literal,
                        route.kind,
                        fallback ?: "-",
                        generation,
                    )
                }
                return CommandRouteResult(
                    route.literal,
                    route.kind,
                    route.definition.name,
                    status,
                    fallback
                )
            }
            if (!takeOver(dispatcher, route, existing, generation)) {
                return CommandRouteResult(
                    route.literal,
                    route.kind,
                    route.definition.name,
                    CommandRouteStatus.TAKEOVER_NOT_CONFIRMED,
                    fallback,
                )
            }
            vanillaAlias = VanillaCommandAliases.aliasFor(route.literal)
        }

        val node = rootBuilder.build(
            route.definition,
            route.literal,
            permissions,
            buildContext,
            route.additionalPermission,
            interceptor,
        )
        dispatcher.root.addChild(node)

        val observed = dispatcher.root.getChild(route.literal)
        val status = if (observed === node) {
            installedInto[route.literal] = node
            CommandRouteStatus.INSTALLED
        } else {
            CommandRouteStatus.INSTALL_MISMATCH
        }
        if (status == CommandRouteStatus.INSTALL_MISMATCH) {
            LOGGER.error(
                "command_route_conflict literal={} route={} reason=install_mismatch fallback={} generation={}",
                route.literal,
                route.kind,
                fallback ?: "-",
                generation,
            )
        }
        return CommandRouteResult(
            route.literal,
            route.kind,
            route.definition.name,
            status,
            fallback,
            vanillaAlias,
        )
    }

    /**
     * Replaces a vanilla literal while keeping it callable at `/minecraft:<literal>`. The original
     * node is detached first, then the alias redirecting to it is installed; if either step fails,
     * vanilla is restored and the takeover is reported as unconfirmed.
     */
    private fun takeOver(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        route: CommandRoute,
        vanilla: CommandNode<CommandSourceStack>,
        generation: Int,
    ): Boolean {
        val alias = VanillaCommandAliases.aliasFor(route.literal)
        if (dispatcher.root.getChild(alias) != null) {
            LOGGER.error(
                "command_takeover literal={} reason=alias_already_present alias={} generation={}",
                route.literal,
                alias,
                generation,
            )
            return false
        }
        if (!treeMutator.removeRootChild(dispatcher, route.literal)) {
            LOGGER.error(
                "command_takeover literal={} reason=remove_failed generation={}",
                route.literal,
                generation,
            )
            return false
        }
        if (!VanillaCommandAliases.install(dispatcher, route.literal, vanilla)) {
            dispatcher.root.addChild(vanilla)
            LOGGER.error(
                "command_takeover literal={} reason=alias_install_failed generation={}",
                route.literal,
                generation,
            )
            return false
        }
        LOGGER.info(
            "command_takeover literal={} alias={} generation={}",
            route.literal,
            alias,
            generation,
        )
        return true
    }

    private fun fallbackFor(route: CommandRoute): String? =
        when (route.kind) {
            CommandRouteKind.ADMIN_ROOT,
            CommandRouteKind.NAMESPACED -> null

            CommandRouteKind.CANONICAL,
            CommandRouteKind.ALIAS,
            CommandRouteKind.CZ_FALLBACK -> CommandRouteName.namespaced(route.definition.name)

            CommandRouteKind.VANILLA_OVERRIDE -> route.takeover?.fallbackLiteral
        }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(CommandRegistry::class.java)

    }

}

internal fun CommandDefinition.toDescriptor(): CommandDescriptor =
    CommandDescriptor(
        literal = name,
        category = category,
        permission = permission,
        playerOnly = playerOnly,
        aliases = aliases,
    )
