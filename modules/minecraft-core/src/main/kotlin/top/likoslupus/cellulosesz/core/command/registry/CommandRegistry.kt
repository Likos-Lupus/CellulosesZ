package top.likoslupus.cellulosesz.core.command.registry

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.core.command.CommandCatalog
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
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

    internal var rootBuilder: CommandRootBuilder =
        CommandRootBuilder(BrigadierCompiler::buildRoot)

    internal constructor(
        definitions: List<CommandDefinition>,
        rootBuilder: CommandRootBuilder,
    ) : this(definitions) {
        this.rootBuilder = rootBuilder
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
                installed
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
        if (existing != null) {
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

        val node = rootBuilder.build(
            route.definition,
            route.literal,
            permissions,
            buildContext,
            route.additionalPermission,
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
            fallback
        )
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
