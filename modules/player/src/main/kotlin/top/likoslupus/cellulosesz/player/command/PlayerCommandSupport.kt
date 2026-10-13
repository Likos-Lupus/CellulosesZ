package top.likoslupus.cellulosesz.player.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages

/**
 * Shared async player orchestration: capture a suspension-safe feedback target, then run the block
 * with it. Never holds a [CommandSourceStack] or a player across suspension.
 */
internal fun playerLaunch(
    source: CommandSourceStack,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    block: suspend (CommandFeedbackTarget) -> Unit,
): Int {
    val playerId = source.player?.uuid
    val target = source.feedbackTarget()
    return when (kernel.launch { block(target) }) {
        null -> source.replyError(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.error.runtime_stopping")
            )
        )

        else -> 1
    }
}

/** Online player names for static suggestions; never touches storage. */
internal fun onlineNames(source: CommandSourceStack): List<String> =
    source.server.playerList.players.map { it.gameProfile.name }
