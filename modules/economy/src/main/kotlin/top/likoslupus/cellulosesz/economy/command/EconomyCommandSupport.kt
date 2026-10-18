package top.likoslupus.cellulosesz.economy.command

import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.DisplayChannel
import top.likoslupus.cellulosesz.core.text.presentation.replyErrorLine
import top.likoslupus.cellulosesz.core.text.presentation.replyLine
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.format.EconomyDisplay
import java.util.*

/** Origin recorded on every ledger transaction a command starts. */
internal const val COMMAND_ORIGIN = "cellulosesz:command"

/** A single-use idempotency key for one command invocation. */
internal fun commandKey(): String =
    "cmd:${UUID.randomUUID()}"

/** The currency formatting of the current settings snapshot. */
internal fun display(settings: EconomySettings): EconomyDisplay =
    EconomyDisplay(settings.currencyName)

internal fun unknownPlayer(
    messages: LocalizedMessages,
    viewerId: UUID?,
    name: String,
): Component =
    messages.renderFor(
        viewerId,
        MessageKey("economy.pay.error.unknown_player"),
        name
    )

/**
 * Shared async economy orchestration: acknowledge immediately, then run the block with the captured
 * feedback target. Never holds a [CommandSourceStack] across suspension.
 */
internal fun economyLaunch(
    source: CommandSourceStack,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
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
            messages.renderFor(
                playerId,
                MessageKey("economy.pending")
            ),
            DisplayChannel.ACTION_BAR
        )
    }
}
