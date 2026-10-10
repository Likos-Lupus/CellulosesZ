package top.likoslupus.cellulosesz.core.command.dsl

/** Pure structural validation of a [CommandDefinition]; returns every problem it finds. */
public object CommandValidator {

    private val LITERAL_PATTERN: Regex = Regex("[a-z0-9_-]+")
    private val ARGUMENT_PATTERN: Regex = Regex("[a-zA-Z0-9_]+")

    public fun validate(definition: CommandDefinition): List<String> {
        val problems = mutableListOf<String>()

        if (!LITERAL_PATTERN.matches(definition.name)) {
            problems += "command name '${definition.name}' must match ${LITERAL_PATTERN.pattern}"
        }

        definition.aliases.forEach { alias ->
            if (!LITERAL_PATTERN.matches(alias)) {
                problems += "alias '$alias' must match ${LITERAL_PATTERN.pattern}"
            }
            if (alias == definition.name) {
                problems += "alias '$alias' duplicates the primary name"
            }
        }

        val executions = mutableListOf<CommandExecution>()
        definition.root.collectExecutions(executions)
        if (executions.isEmpty()) {
            problems += "command '${definition.name}' declares no executor"
        }

        if (definition.sourceAccess == SourceAccess.NON_PLAYER &&
            definition.root.execution?.playerRequired == true
        ) {
            problems += "command '${definition.name}' is player-only but restricted to non-player sources"
        }

        validateChildren(
            definition.root.children,
            path = definition.name,
            inheritedNonPlayer = definition.sourceAccess == SourceAccess.NON_PLAYER,
            problems = problems,
        )

        return problems
    }

    private fun validateChildren(
        children: List<CommandNodeSpec>,
        path: String,
        inheritedNonPlayer: Boolean,
        problems: MutableList<String>,
    ) {
        val seen = mutableMapOf<String, Int>()
        children.forEach { child ->
            seen.merge(child.name, 1, Int::plus)

            val childPath = "$path ${child.name}"
            val pattern = when (child) {
                is ArgumentNodeSpec<*> -> ARGUMENT_PATTERN
                else -> LITERAL_PATTERN
            }
            if (!pattern.matches(child.name)) {
                problems += "node '$childPath' name must match ${pattern.pattern}"
            }

            val nonPlayer = inheritedNonPlayer
                    || child.sourceAccess == SourceAccess.NON_PLAYER
            if (nonPlayer
                && child.sourceAccess == SourceAccess.PLAYER
            ) {
                problems += "node '$childPath' requires a player under a non-player path"
            }
            child.execution?.let { execution ->
                if (nonPlayer && execution.playerRequired) {
                    problems += "node '$childPath' is player-only under a non-player path"
                }
            }

            if (child is ArgumentNodeSpec<*>
                && child.argument.greedy
                && child.children.isNotEmpty()
            ) {
                problems += "greedy argument '$childPath' cannot have child nodes"
            }

            if (child.execution == null
                && child.children.isEmpty()
            ) {
                problems += "node '$childPath' has no executor and no children"
            }

            validateChildren(
                child.children,
                childPath,
                nonPlayer,
                problems
            )
        }
        seen.filterValues { it > 1 }.keys.forEach {
            problems += "duplicate child '$it' under '$path'"
        }
    }

    private fun CommandNodeSpec.collectExecutions(into: MutableList<CommandExecution>) {
        execution?.let(into::add)
        children.forEach { it.collectExecutions(into) }
    }

}
