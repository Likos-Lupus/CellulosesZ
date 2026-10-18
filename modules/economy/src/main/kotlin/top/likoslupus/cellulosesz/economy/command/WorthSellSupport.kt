package top.likoslupus.cellulosesz.economy.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.economy.format.EconomyDisplay
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import java.util.*

/** The currency formatting of the current settings snapshot. */
internal fun WorthSellContext.display(): EconomyDisplay =
    EconomyDisplay(settings().currencyName)

internal fun WorthSellContext.disabled(
    source: CommandSourceStack,
    playerId: UUID?
): Int =
    source.replyNotice(
        adventure,
        presentation,
        playerId,
        EconomyMessages.disabled()
    )

internal suspend fun WorthSellContext.deliver(
    target: CommandFeedbackTarget,
    playerId: UUID?,
    notice: Notice,
) {
    kernel.sendNotices(
        target,
        adventure,
        presentation,
        playerId,
        listOf(notice)
    )
}
