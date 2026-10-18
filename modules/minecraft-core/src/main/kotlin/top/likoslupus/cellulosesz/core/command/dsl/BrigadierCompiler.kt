package top.likoslupus.cellulosesz.core.command.dsl

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.CommandInterceptor
import top.likoslupus.cellulosesz.core.command.CommandInvocation
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import java.util.function.Predicate

/**
 * Compiles an immutable [CommandDefinition] into a single real Mojang Brigadier root node for one
 * literal. It never touches a dispatcher: the registry decides whether the literal is free, installs
 * the node and verifies identity afterwards. Aliases/canonical/namespaced forms are each built as
 * their own independent tree from the same declaration; no Brigadier redirect is used and no two
 * routes share a mutable node instance.
 */
public object BrigadierCompiler {

    public fun buildRoot(
        definition: CommandDefinition,
        literal: String,
        permissions: PermissionService,
        buildContext: CommandBuildContext,
        additionalPermission: PermissionSpec? = null,
        interceptor: CommandInterceptor? = null,
    ): LiteralCommandNode<CommandSourceStack> {
        val builder = Commands.literal(literal)
        // Mojang Brigadier's `requires` is a *setter*, not additive: calling it twice silently drops
        // the first predicate. Source access and the permission node must therefore be ANDed into a
        // single predicate and installed exactly once, or a source restriction (e.g. NON_PLAYER
        // `/sudo`) would be overwritten by the permission check.
        builder.requires(
            combinedRequirement(
                access = definition.sourceAccess,
                permission = definition.permission,
                permissions = permissions,
                additionalPermission = additionalPermission,
            )
        )
        definition.root.execution?.let {
            builder.executes(
                executor(
                    it,
                    emptyList(),
                    definition.name,
                    interceptor,
                )
            )
        }
        appendChildren(
            builder,
            definition.root.children,
            permissions,
            buildContext,
            emptyList(),
            definition.name,
            interceptor,
        )
        return builder.build()
    }

    private fun appendChildren(
        parent: ArgumentBuilder<CommandSourceStack, *>,
        children: List<CommandNodeSpec>,
        permissions: PermissionService,
        buildContext: CommandBuildContext,
        ancestors: List<ArgumentKey<*>>,
        commandName: String,
        interceptor: CommandInterceptor?,
    ) {
        children.forEach { child ->
            when (child) {
                is LiteralNodeSpec -> {
                    val builder = Commands.literal(child.name)
                    applyRequirement(
                        builder,
                        child.permission,
                        child.sourceAccess,
                        permissions
                    )
                    child.execution?.let {
                        builder.executes(
                            executor(
                                it,
                                ancestors,
                                commandName,
                                interceptor,
                            )
                        )
                    }
                    appendChildren(
                        builder,
                        child.children,
                        permissions,
                        buildContext,
                        ancestors,
                        commandName,
                        interceptor,
                    )
                    parent.then(builder)
                }

                is ArgumentNodeSpec<*> -> appendArgument(
                    parent,
                    child,
                    permissions,
                    buildContext,
                    ancestors,
                    commandName,
                    interceptor,
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun appendArgument(
        parent: ArgumentBuilder<CommandSourceStack, *>,
        child: ArgumentNodeSpec<*>,
        permissions: PermissionService,
        buildContext: CommandBuildContext,
        ancestors: List<ArgumentKey<*>>,
        commandName: String,
        interceptor: CommandInterceptor?,
    ) {
        val spec = child as ArgumentNodeSpec<Any>
        val builder = Commands.argument(
            spec.name,
            spec.argument.createType(buildContext)
        )
        applyRequirement(
            builder,
            spec.permission,
            spec.sourceAccess,
            permissions
        )
        val keys = ancestors + spec.key
        spec.suggestion?.let {
            builder.suggests(
                provider(it)
            )
        }
        spec.execution?.let {
            builder.executes(
                executor(
                    it,
                    keys,
                    commandName,
                    interceptor,
                )
            )
        }
        appendChildren(
            builder,
            spec.children,
            permissions,
            buildContext,
            keys,
            commandName,
            interceptor,
        )
        parent.then(builder)
    }

    private fun applyRequirement(
        builder: ArgumentBuilder<CommandSourceStack, *>,
        permission: PermissionSpec?,
        access: SourceAccess,
        permissions: PermissionService,
    ) {
        builder.requires(
            combinedRequirement(
                access,
                permission,
                permissions,
                additionalPermission = null
            )
        )
    }

    /**
     * The single requirement predicate for a node. Brigadier's `requires` is a setter, so every
     * constraint a node must satisfy (source access AND its permission node AND, for a route-level
     * vanilla takeover, the extra override permission) is combined here and installed with one call;
     * never call `requires` more than once on the same builder.
     */
    private fun combinedRequirement(
        access: SourceAccess,
        permission: PermissionSpec?,
        permissions: PermissionService,
        additionalPermission: PermissionSpec?,
    ): Predicate<CommandSourceStack> =
        combineRequirements(
            accessAllowed = { source -> matchesAccess(source, access) },
            permissionAllowed = { source ->
                (permission == null || permissions.allows(source, permission)) &&
                        (additionalPermission == null || permissions.allows(
                            source,
                            additionalPermission
                        ))
            },
        )

    private fun executor(
        execution: CommandExecution,
        arguments: List<ArgumentKey<*>>,
        commandName: String,
        interceptor: CommandInterceptor?,
    ): Command<CommandSourceStack> =
        Command { context ->
            val action = { execution.action(ExecutionScope(context.source, context, arguments)) }
            if (interceptor == null) {
                action()
            } else {
                var executed = false
                var result = 1
                interceptor.intercept(CommandInvocation(commandName, context.source)) {
                    executed = true
                    result = action()
                }
                // A gate that defers leaves the body unrun; Brigadier still needs a synchronous result.
                if (executed) result else 1
            }
        }

    private fun provider(suggestion: CommandSuggestion): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { context, builder ->
            val scope = SuggestionScope(context.source)
            suggestion.suggest(scope)
            scope.snapshot().forEach(builder::suggest)
            builder.buildFuture()
        }

    internal fun matchesAccess(source: CommandSourceStack, access: SourceAccess): Boolean =
        when (access) {
            SourceAccess.ANY -> true
            SourceAccess.PLAYER -> source.player != null
            SourceAccess.NON_PLAYER -> source.player == null
        }

}

/**
 * Combines a source-access gate and a permission gate into a single Brigadier requirement.
 *
 * Mojang Brigadier's `requires` is a *setter*: installing source access and permission with two
 * separate calls silently drops the first predicate, which would let a source restriction (for
 * example the NON_PLAYER `/sudo` path) be overwritten by the permission check. This is the only
 * supported way to build a node requirement, and it is generic over the source type so the AND
 * contract can be unit-tested without bootstrapping Minecraft.
 */
internal fun <S> combineRequirements(
    accessAllowed: (S) -> Boolean,
    permissionAllowed: (S) -> Boolean,
): Predicate<S> =
    Predicate { source ->
        accessAllowed(source) && permissionAllowed(source)
    }
