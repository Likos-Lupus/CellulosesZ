package top.likoslupus.cellulosesz.utility.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

/**
 * Shared async utility orchestration: acknowledge immediately, then run the block with the captured
 * feedback target. Never holds a [CommandSourceStack] across suspension.
 */
internal fun utilityLaunch(
    source: CommandSourceStack,
    kernel: RuntimeKernel,
    pending: String,
    block: suspend (CommandFeedbackTarget) -> Unit,
): Int {
    val target = source.feedbackTarget()
    return when (kernel.launch { block(target) }) {
        null -> source.replyError(Messages.prefixed("runtime is shutting down"))
        else -> source.reply(Messages.prefixed(pending))
    }
}
