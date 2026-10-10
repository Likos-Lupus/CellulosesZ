package top.likoslupus.cellulosesz.core.command.dsl

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.text.Messages

/**
 * Declares one top-level command. Root metadata is passed here; the trailing block declares only
 * syntax (nodes, arguments, completions, executors). The result is validated before it is returned.
 */
public fun command(
    name: String,
    category: CommandCategory,
    permission: PermissionSpec,
    documentation: String,
    aliases: Set<String> = emptySet(),
    sourceAccess: SourceAccess = SourceAccess.ANY,
    syntax: CommandSyntax.() -> Unit,
): CommandDefinition {
    val scope = NodeScope()
    scope.syntax()

    val root = LiteralNodeSpec(
        name = name,
        permission = null,
        documentation = null,
        sourceAccess = SourceAccess.ANY,
        execution = scope.execution,
        children = scope.children.toList(),
    )
    val definition = CommandDefinition(
        name = name,
        category = category,
        permission = permission,
        documentation = CommandDocumentId.of(documentation),
        aliases = aliases,
        sourceAccess = sourceAccess,
        playerOnly = root.playerOnly(),
        root = root,
    )

    val problems = CommandValidator.validate(definition)
    require(problems.isEmpty()) {
        "invalid command declaration '$name': ${problems.joinToString("; ")}"
    }
    return definition
}

internal fun CommandNodeSpec.playerOnly(): Boolean {
    val executions = mutableListOf<CommandExecution>()
    collectExecutions(executions)
    return executions.isNotEmpty() && executions.all { it.playerRequired }
}

private fun CommandNodeSpec.collectExecutions(into: MutableList<CommandExecution>) {
    execution?.let(into::add)
    children.forEach { it.collectExecutions(into) }
}

internal open class NodeScope : CommandSyntax {

    internal val children: MutableList<CommandNodeSpec> = mutableListOf()
    internal var execution: CommandExecution? = null

    override fun literal(
        name: String,
        permission: PermissionSpec?,
        documentation: String?,
        sourceAccess: SourceAccess,
        syntax: CommandSyntax.() -> Unit,
    ) {
        val child = NodeScope()
        child.syntax()
        children += LiteralNodeSpec(
            name = name,
            permission = permission,
            documentation = documentation?.let(CommandDocumentId::of),
            sourceAccess = sourceAccess,
            execution = child.execution,
            children = child.children.toList(),
        )
    }

    override fun <T : Any> argument(
        name: String,
        type: CommandArgument<T>,
        permission: PermissionSpec?,
        sourceAccess: SourceAccess,
        syntax: ArgumentSyntax<T>.(ArgumentKey<T>) -> Unit,
    ) {
        val key = ArgumentKey(name, type)
        val child = ArgumentScope<T>()
        child.syntax(key)
        children += ArgumentNodeSpec(
            name = name,
            argument = type,
            key = key,
            permission = permission,
            documentation = null,
            sourceAccess = sourceAccess,
            execution = child.execution,
            suggestion = child.suggestion,
            children = child.children.toList(),
        )
    }

    override fun executes(action: ExecutionScope.() -> Int) {
        setExecution(CommandExecution(playerRequired = false) { scope -> action(scope) })
    }

    override fun executesPlayer(action: PlayerExecutionScope.() -> Int) {
        setExecution(
            CommandExecution(playerRequired = true) {
                when (val player = it.source.player) {
                    null -> it.source.replyError(Messages.prefixed("this command requires a player"))
                    else -> action(
                        PlayerExecutionScope(
                            it.source,
                            player.uuid,
                            it
                        )
                    )
                }
            }
        )
    }

    private fun setExecution(next: CommandExecution) {
        check(execution == null) {
            "a command node may declare at most one executes/executesPlayer"
        }
        execution = next
    }

}

internal class ArgumentScope<T : Any> : NodeScope(), ArgumentSyntax<T> {

    internal var suggestion: CommandSuggestion? = null

    override fun suggests(block: SuggestionScope.() -> Unit) {
        check(suggestion == null) {
            "a command node may declare at most one suggests block"
        }
        suggestion = CommandSuggestion { scope -> block(scope) }
    }

}
