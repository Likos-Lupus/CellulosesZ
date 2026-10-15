package top.likoslupus.cellulosesz.core.command.dsl

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
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
    ): LiteralCommandNode<CommandSourceStack> {
        val builder = Commands.literal(literal)
        if (definition.sourceAccess != SourceAccess.ANY) {
            builder.requires(accessRequirement(definition.sourceAccess))
        }
        builder.requires(permissionRequirement(definition.permission, permissions))
        definition.root.execution?.let {
            builder.executes(
                executor(
                    it,
                    emptyList()
                )
            )
        }
        appendChildren(
            builder,
            definition.root.children,
            permissions,
            buildContext,
            emptyList()
        )
        return builder.build()
    }

    private fun appendChildren(
        parent: ArgumentBuilder<CommandSourceStack, *>,
        children: List<CommandNodeSpec>,
        permissions: PermissionService,
        buildContext: CommandBuildContext,
        ancestors: List<ArgumentKey<*>>,
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
                                ancestors
                            )
                        )
                    }
                    appendChildren(
                        builder,
                        child.children,
                        permissions,
                        buildContext,
                        ancestors
                    )
                    parent.then(builder)
                }

                is ArgumentNodeSpec<*> -> appendArgument(
                    parent,
                    child,
                    permissions,
                    buildContext,
                    ancestors,
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
                    keys
                )
            )
        }
        appendChildren(
            builder,
            spec.children,
            permissions,
            buildContext,
            keys
        )
        parent.then(builder)
    }

    private fun applyRequirement(
        builder: ArgumentBuilder<CommandSourceStack, *>,
        permission: PermissionSpec?,
        access: SourceAccess,
        permissions: PermissionService,
    ) {
        if (access != SourceAccess.ANY) {
            builder.requires(accessRequirement(access))
        }
        if (permission != null) {
            builder.requires(permissionRequirement(permission, permissions))
        }
    }

    private fun executor(
        execution: CommandExecution,
        arguments: List<ArgumentKey<*>>,
    ): Command<CommandSourceStack> =
        Command {
            execution.action(ExecutionScope(it.source, it, arguments))
        }

    private fun provider(suggestion: CommandSuggestion): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { context, builder ->
            val scope = SuggestionScope(context.source)
            suggestion.suggest(scope)
            scope.snapshot().forEach(builder::suggest)
            builder.buildFuture()
        }

    private fun permissionRequirement(
        spec: PermissionSpec,
        permissions: PermissionService,
    ): Predicate<CommandSourceStack> =
        Predicate { permissions.allows(it, spec) }

    private fun accessRequirement(access: SourceAccess): Predicate<CommandSourceStack> =
        Predicate { matchesAccess(it, access) }

    internal fun matchesAccess(source: CommandSourceStack, access: SourceAccess): Boolean =
        when (access) {
            SourceAccess.ANY -> true
            SourceAccess.PLAYER -> source.player != null
            SourceAccess.NON_PLAYER -> source.player == null
        }

}
