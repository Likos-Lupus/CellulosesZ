package top.likoslupus.cellulosesz.core.text.presentation

import net.kyori.adventure.text.Component
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.InformationOutput
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticePriority
import java.util.*

/** Sends one rendered notice to the command source synchronously; returns the Brigadier result. */
public fun CommandSourceStack.replyNotice(
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    playerId: UUID?,
    notice: Notice,
    channel: DisplayChannel = DisplayChannel.CHAT,
): Int =
    InformationOutput(adventure).reply(
        this,
        presentation.renderFor(playerId, notice),
        effectiveChannel(channel, listOf(notice)),
    )

/** Sends several notices (for example success plus a warning) synchronously. */
public fun CommandSourceStack.replyNotices(
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    playerId: UUID?,
    notices: List<Notice>,
    channel: DisplayChannel = DisplayChannel.CHAT,
): Int =
    InformationOutput(adventure).reply(
        this,
        notices.flatMap { presentation.renderFor(playerId, it) },
        effectiveChannel(channel, notices),
    )

public fun CommandSourceStack.replyPage(
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    playerId: UUID?,
    page: ListPage,
): Int =
    InformationOutput(adventure).reply(
        this,
        presentation.renderFor(playerId, page)
    )

public fun CommandSourceStack.replyDetail(
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    playerId: UUID?,
    detail: DetailView,
): Int =
    InformationOutput(adventure).reply(
        this,
        presentation.renderFor(playerId, detail)
    )

/** Synchronous single-line reply on an explicit channel (used for pending acknowledgements). */
public fun CommandSourceStack.replyLine(
    adventure: AdventureRuntime,
    component: Component,
    channel: DisplayChannel = DisplayChannel.CHAT,
): Int =
    InformationOutput(adventure).reply(
        this,
        listOf(component),
        channel
    )

/** Synchronous single-line failure reply on an explicit channel; returns 0. */
public fun CommandSourceStack.replyErrorLine(
    adventure: AdventureRuntime,
    component: Component,
    channel: DisplayChannel = DisplayChannel.CHAT,
): Int =
    InformationOutput(adventure)
            .replyError(
                this,
                listOf(component),
                channel
            )

/** Delivers notices to a captured target on the server thread, resolved per recipient language. */
public suspend fun RuntimeKernel.sendNotices(
    target: CommandFeedbackTarget,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    playerId: UUID?,
    notices: List<Notice>,
    channel: DisplayChannel = DisplayChannel.CHAT,
) {
    InformationOutput(
        adventure,
        this
    ).deliver(
        target,
        notices.flatMap { presentation.renderFor(playerId, it) },
        effectiveChannel(channel, notices),
    )
}

/** Delivers a list page to a captured target on the server thread. */
public suspend fun RuntimeKernel.sendPage(
    target: CommandFeedbackTarget,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    playerId: UUID?,
    page: ListPage,
) {
    InformationOutput(
        adventure,
        this
    ).deliver(
        target,
        presentation.renderFor(playerId, page)
    )
}

private fun effectiveChannel(requested: DisplayChannel, notices: List<Notice>): DisplayChannel =
    when {
        notices.any {
            it.priority == NoticePriority.HIGH
                    || it.priority == NoticePriority.CRITICAL
        } -> DisplayChannel.CHAT

        else -> requested
    }
