package top.likoslupus.cellulosesz.core.command.dsl

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import java.util.function.Predicate

/** The Brigadier node(s) registered for one definition (canonical name plus aliases). */
public class RegisteredCommand internal constructor(
    public val definition: CommandDefinition,
    public val roots: List<LiteralCommandNode<CommandSourceStack>>,
)

/**
 * Compiles an immutable [CommandDefinition] into the real Mojang Brigadier tree and registers it
 * into the current dispatcher. Aliases are compiled as equivalent trees from the same declaration;
 * no Brigadier redirect is used.
 */
public object BrigadierCompiler {

    public fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        definition: CommandDefinition,
        permissions: PermissionService,
        buildContext: CommandBuildContext,
    ): RegisteredCommand {
        val roots = mutableListOf<LiteralCommandNode<CommandSourceStack>>()
        val names = buildList {
            add(definition.name)
            addAll(definition.aliases.sorted())
        }

        names.forEach { name ->
            val builder = Commands.literal(name)
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
            roots += dispatcher.register(builder)
        }

        return RegisteredCommand(definition, roots)
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
