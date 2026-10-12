package top.likoslupus.cellulosesz.administration.moderation.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.presentation.DisplayChannel
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.foundation.time.DurationParser
import top.likoslupus.cellulosesz.foundation.time.TemporaryDuration

internal val ModerationActor.displayName: String
    get() = when (this) {
        ModerationActor.Console -> "Console"
        is ModerationActor.Player -> name
    }

internal fun resolveReason(
    raw: String?,
    default: String,
    maxLength: Int,
): ModerationReason? =
    ModerationReason.parse(
        raw ?: default,
        maxLength
    )

internal fun durationFor(
    context: CommandContext<CommandSourceStack>,
    temporary: Boolean,
): TemporaryDuration? =
    when {
        !temporary -> null
        else -> DurationParser.parse(
            StringArgumentType.getString(
                context,
                "duration"
            )
        )
    }

internal fun invalidReason(
    source: CommandSourceStack,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int =
    source.replyError(
        adventure,
        messages.renderFor(
            source.player?.uuid,
            MessageKey("administration.error.invalid_reason")
        ),
    )

internal fun invalidDuration(
    source: CommandSourceStack,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int =
    source.replyError(
        adventure,
        messages.renderFor(
            source.player?.uuid,
            MessageKey("administration.error.invalid_duration")
        ),
    )

/**
 * Server-thread notification used after an action has been committed. The semantic notices are
 * rendered once per moderator by [ModerationNotifier].
 */
internal suspend fun notifyModerators(
    kernel: RuntimeKernel,
    notifier: ModerationNotifier,
    notices: List<Notice>,
) {
    kernel.onServerThread { notifier.notify(notices) }
}

/**
 * Shared async moderation orchestration: acknowledge immediately, then run the block with the
 * captured feedback target. Never holds a [CommandSourceStack] across suspension.
 */
internal fun moderationLaunch(
    source: CommandSourceStack,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    pending: LocalizedText,
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
            ),
        )

        else -> source.replyNotice(
            adventure,
            presentation,
            playerId,
            Notice(pending, NoticeTone.INFO),
            DisplayChannel.ACTION_BAR,
        )
    }
}
