package top.likoslupus.cellulosesz.core.command.dsl

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/**
 * A compiled, immutable command declaration. Constructed only by [command]; there is no public
 * `copy`, so a definition can never be mutated after validation. The Brigadier tree is rebuilt from
 * this on every registration so it is never bound to a stale dispatcher.
 */
public class CommandDefinition internal constructor(
    public val name: String,
    public val category: CommandCategory,
    public val permission: PermissionSpec,
    public val documentation: CommandDocumentId,
    public val aliases: Set<String>,
    public val sourceAccess: SourceAccess,
    public val playerOnly: Boolean,
    internal val root: LiteralNodeSpec,
)

internal sealed interface CommandNodeSpec {

    val name: String
    val permission: PermissionSpec?
    val documentation: CommandDocumentId?
    val sourceAccess: SourceAccess
    val execution: CommandExecution?
    val children: List<CommandNodeSpec>

}

internal class LiteralNodeSpec(
    override val name: String,
    override val permission: PermissionSpec?,
    override val documentation: CommandDocumentId?,
    override val sourceAccess: SourceAccess,
    override val execution: CommandExecution?,
    override val children: List<CommandNodeSpec>,
) : CommandNodeSpec

internal class ArgumentNodeSpec<T : Any>(
    override val name: String,
    val argument: CommandArgument<T>,
    val key: ArgumentKey<T>,
    override val permission: PermissionSpec?,
    override val documentation: CommandDocumentId?,
    override val sourceAccess: SourceAccess,
    override val execution: CommandExecution?,
    val suggestion: CommandSuggestion?,
    override val children: List<CommandNodeSpec>,
) : CommandNodeSpec

/** A synchronous Brigadier executor. [playerRequired] is recorded so the validator can reject
 * player-only executors on a non-player path. */
internal class CommandExecution(
    val playerRequired: Boolean,
    val action: (ExecutionScope) -> Int,
)

internal fun interface CommandSuggestion {

    fun suggest(scope: SuggestionScope)

}
