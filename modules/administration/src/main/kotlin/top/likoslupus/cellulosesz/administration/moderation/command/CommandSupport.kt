package top.likoslupus.cellulosesz.administration.moderation.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.administration.moderation.DurationParser
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.TemporaryDuration
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

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

internal fun invalidReason(source: CommandSourceStack): Int =
    source.replyError(
        Messages.prefixed(
            "invalid moderation reason"
        )
    )

internal fun invalidDuration(source: CommandSourceStack): Int =
    source.replyError(
        Messages.prefixed(
            "invalid duration; use s, m, h, d, w (for example 1d12h)"
        )
    )

/**
 * Server-thread notification used after an action has been committed.
 */
internal suspend fun notifyModerators(
    kernel: RuntimeKernel,
    notifier: ModerationNotifier,
    message: String,
) {
    kernel.onServerThread { notifier.notify(message) }
}

/**
 * Shared async moderation orchestration: acknowledge immediately, then deliver the final component
 * to the captured feedback target. Never holds a [CommandSourceStack] across suspension.
 */
internal fun moderationLaunch(
    source: CommandSourceStack,
    kernel: RuntimeKernel,
    pending: String,
    block: suspend () -> Component,
): Int =
    when (
        kernel.launch {
            kernel.message(
                target = source.feedbackTarget(),
                message = block()
            )
        }
    ) {
        null -> source.replyError(Messages.prefixed("runtime is shutting down"))
        else -> source.reply(Messages.prefixed(pending))
    }
