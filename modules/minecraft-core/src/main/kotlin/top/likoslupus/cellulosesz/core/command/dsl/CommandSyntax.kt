package top.likoslupus.cellulosesz.core.command.dsl

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import java.util.*

/**
 * The syntax-declaration surface. A `{}` block only declares nodes, arguments, completions and
 * executors; it never carries metadata setters. Root metadata lives on [command].
 */
@CommandDslMarker
public interface CommandSyntax {

    public fun literal(
        name: String,
        permission: PermissionSpec? = null,
        documentation: String? = null,
        sourceAccess: SourceAccess = SourceAccess.ANY,
        syntax: CommandSyntax.() -> Unit,
    )

    public fun <T : Any> argument(
        name: String,
        type: CommandArgument<T>,
        permission: PermissionSpec? = null,
        sourceAccess: SourceAccess = SourceAccess.ANY,
        syntax: ArgumentSyntax<T>.(ArgumentKey<T>) -> Unit,
    )

    public fun executes(action: ExecutionScope.() -> Int)

    public fun executesPlayer(action: PlayerExecutionScope.() -> Int)

}

/** Argument scopes additionally allow a completion provider; other scopes cannot declare one. */
@CommandDslMarker
public interface ArgumentSyntax<T : Any> : CommandSyntax {

    public fun suggests(block: SuggestionScope.() -> Unit)

}

/**
 * Execution context for a synchronous executor. Argument values are bound from the Brigadier
 * context by their own typed accessor; a key that is not on the executing path is a programming
 * error, not a user error.
 */
public class ExecutionScope internal constructor(
    public val source: CommandSourceStack,
    public val context: CommandContext<CommandSourceStack>,
    private val arguments: List<ArgumentKey<*>>,
) {

    public fun <T : Any> get(key: ArgumentKey<T>): T {
        require(key in arguments) {
            "argument '${key.name}' is not available on this command path"
        }
        return key.argument.read(context, key.name)
    }

}

/**
 * Execution context for [CommandSyntax.executesPlayer]. The player check and the "requires a
 * player" feedback already happened; [playerId] is therefore non-null.
 */
public class PlayerExecutionScope internal constructor(
    public val source: CommandSourceStack,
    public val playerId: UUID,
    private val delegate: ExecutionScope,
) {

    public val context: CommandContext<CommandSourceStack>
        get() = delegate.context

    public fun <T : Any> get(key: ArgumentKey<T>): T =
        delegate.get(key)

}

/** Collects static suggestions. It must never touch blocking storage. */
public class SuggestionScope internal constructor(
    public val source: CommandSourceStack,
) {

    private val values: LinkedHashSet<String> = LinkedHashSet()

    public fun suggest(value: String) {
        values += value
    }

    public fun suggestAll(candidates: Iterable<String>) {
        values += candidates
    }

    internal fun snapshot(): List<String> =
        values.toList()

}
