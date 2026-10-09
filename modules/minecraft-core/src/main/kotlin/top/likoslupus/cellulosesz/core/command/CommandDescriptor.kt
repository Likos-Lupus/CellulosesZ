package top.likoslupus.cellulosesz.core.command

import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/** The bounded context (or root command) that owns a [CommandDescriptor]. */
public enum class CommandCategory {

    MOVEMENT,
    COMMUNICATION,
    ADMINISTRATION,
    UTILITY,
    ROOT,

}

/**
 * Declarative metadata for one top-level command. This is a catalog entry, not a registration: the
 * Brigadier tree is still built explicitly by each command object. It exists so the permission node
 * catalog, the README command table and the actual registrations can be checked against one source
 * of truth instead of drifting.
 *
 * [playerOnly] is true when every execution path requires the executor to be a player.
 */
public data class CommandDescriptor(
    public val literal: String,
    public val category: CommandCategory,
    public val permission: PermissionSpec,
    public val playerOnly: Boolean,
    public val aliases: Set<String> = emptySet(),
)

/** Pure, dependency-free validation shared by the aggregated catalog tests. */
public object CommandCatalog {

    /** Returns every problem found in [descriptors]; empty means the catalog is well-formed. */
    public fun validate(descriptors: List<CommandDescriptor>): List<String> {
        val problems = mutableListOf<String>()
        val literals = mutableMapOf<String, Int>()
        val aliases = mutableMapOf<String, Int>()
        val nodes = mutableMapOf<String, Int>()

        descriptors.forEach { descriptor ->
            literals.merge(
                descriptor.literal,
                1,
                Int::plus
            )
            nodes.merge(
                descriptor.permission.node,
                1,
                Int::plus
            )
            descriptor.aliases.forEach {
                aliases.merge(
                    it,
                    1,
                    Int::plus
                )
            }

            if (descriptor.literal != descriptor.literal.lowercase()) {
                problems += "command literal '${descriptor.literal}' must be lowercase"
            }
            if (descriptor.permission.node != descriptor.permission.node.lowercase()) {
                problems += "permission node '${descriptor.permission.node}' must be lowercase"
            }
            if (!descriptor.permission.node.startsWith("cellulosesz.")) {
                problems += "permission node '${descriptor.permission.node}' must be cellulosesz-prefixed"
            }
        }

        literals.filterValues { it > 1 }.keys
                .forEach { problems += "duplicate command literal '$it'" }
        aliases.filterValues { it > 1 }.keys
                .forEach { problems += "duplicate command alias '$it'" }
        nodes.filterValues { it > 1 }.keys
                .forEach { problems += "duplicate permission node '$it'" }
        (literals.keys intersect aliases.keys)
                .forEach { problems += "alias '$it' conflicts with a command literal" }

        return problems
    }

}
