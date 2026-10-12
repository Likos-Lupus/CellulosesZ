package top.likoslupus.cellulosesz.communication.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.*
import java.util.*

/**
 * Shared async communication orchestration: acknowledge immediately, then run the block with the
 * captured feedback target. Never holds a [CommandSourceStack] across suspension.
 */
internal fun communicationLaunch(
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

/**
 * Renders a semantic [Notice] for the given recipient and replies synchronously. A success result
 * keeps the Brigadier success flag; a failure result replies as an error.
 */
internal fun CommandSourceStack.replyNoticeOutcome(
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    playerId: UUID?,
    notice: Notice,
    success: Boolean,
): Int =
    when {
        success -> replyNotice(adventure, presentation, playerId, notice)
        else -> {
            presentation.renderFor(playerId, notice).forEach {
                replyError(adventure, it)
            }
            0
        }
    }
