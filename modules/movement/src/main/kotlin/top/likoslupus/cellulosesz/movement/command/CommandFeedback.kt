package top.likoslupus.cellulosesz.movement.command

import kotlinx.coroutines.Job
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.text.Messages

/**
 * Shared Brigadier return for asynchronous movement commands: acknowledge immediately, or report a
 * shutting-down runtime when the kernel refused the task.
 */
internal fun launchResult(
    source: CommandSourceStack,
    job: Job?,
    pending: String
): Int =
    if (job == null) {
        source.replyError(Messages.prefixed("runtime is shutting down"))
    } else {
        source.reply(Messages.prefixed(pending))
    }
