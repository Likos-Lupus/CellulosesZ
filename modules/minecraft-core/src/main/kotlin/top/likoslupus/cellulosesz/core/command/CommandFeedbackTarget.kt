package top.likoslupus.cellulosesz.core.command

import net.minecraft.commands.CommandSourceStack
import java.util.*

/**
 * A stable, suspension-safe command feedback destination. Async command orchestration captures this
 * instead of a [CommandSourceStack] or [net.minecraft.server.level.ServerPlayer].
 */
public sealed interface CommandFeedbackTarget {

    public data class Player(
        public val id: UUID,
    ) : CommandFeedbackTarget

    public data object Server : CommandFeedbackTarget

}

/** Snapshots the source's feedback destination. Never hold the source across suspension. */
public fun CommandSourceStack.feedbackTarget(): CommandFeedbackTarget =
    player?.let { CommandFeedbackTarget.Player(it.uuid) }
        ?: CommandFeedbackTarget.Server
