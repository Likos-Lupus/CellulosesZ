package top.likoslupus.cellulosesz.core.command

import net.minecraft.commands.CommandSourceStack
import java.util.*

/**
 * One interception point in front of every CellulosesZ command body. An interceptor decides whether
 * and when the body runs: it may call [proceed] immediately, after an asynchronous step (for example a
 * ledger charge), or never (a denial). The contract is deliberately tiny so gates compose in order
 * without a mutable listener registry.
 *
 * [proceed] must be called **at most once** and, when it is used, from the server thread: the command
 * body mutates server state. A gate that resumes after a suspension is responsible for hopping back
 * with `RuntimeKernel.onServerThread` before calling it.
 */
public fun interface CommandInterceptor {

    public fun intercept(invocation: CommandInvocation, proceed: () -> Unit)

}

/**
 * The facts a [CommandInterceptor] may inspect before a command body runs. [commandName] is the
 * canonical declaration name, so an alias and the namespaced fallback of the same command share one
 * gate identity and are charged or cooldowned once per declaration.
 */
public class CommandInvocation(
    public val commandName: String,
    public val source: CommandSourceStack,
) {

    public val playerId: UUID? = source.player?.uuid

}
