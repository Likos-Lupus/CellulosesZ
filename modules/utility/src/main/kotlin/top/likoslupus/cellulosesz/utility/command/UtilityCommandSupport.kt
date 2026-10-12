package top.likoslupus.cellulosesz.utility.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.DisplayChannel
import top.likoslupus.cellulosesz.core.text.presentation.replyErrorLine
import top.likoslupus.cellulosesz.core.text.presentation.replyLine

/**
 * Shared async utility orchestration: acknowledge immediately, then run the block with the captured
 * feedback target. Never holds a [CommandSourceStack] across suspension.
 */
internal fun utilityLaunch(
    source: CommandSourceStack,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    pending: MessageKey,
    block: suspend (CommandFeedbackTarget) -> Unit,
): Int {
    val playerId = source.player?.uuid
    val target = source.feedbackTarget()
    return when (kernel.launch { block(target) }) {
        null -> source.replyErrorLine(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.error.runtime_stopping")
            )
        )

        else -> source.replyLine(
            adventure,
            messages.renderFor(playerId, pending),
            DisplayChannel.ACTION_BAR
        )
    }
}
