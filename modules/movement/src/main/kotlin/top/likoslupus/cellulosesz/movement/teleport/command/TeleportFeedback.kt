package top.likoslupus.cellulosesz.movement.teleport.command

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome

/**
 * Single place that turns a failure outcome into user-facing text. Commands only add the success
 * message for their own context, so a growing outcome set never gets re-mapped in every command.
 */
internal object TeleportFeedback {

    /** Returns the failure message, or null when the player is gone and should not be messaged. */
    fun failure(outcome: TeleportOutcome): Component? = Messages.prefixed(
        when (outcome) {
            is TeleportOutcome.Success -> return null
            TeleportOutcome.PlayerOffline -> "you are no longer online"
            TeleportOutcome.TargetOffline -> "the target is no longer online"
            is TeleportOutcome.UnknownDimension -> "world '${outcome.dimension}' is unavailable"
            TeleportOutcome.OutsideWorldBorder -> "destination is outside the world border"
            TeleportOutcome.UnsafeDestination -> "destination is not safe"
            is TeleportOutcome.Cooldown -> "teleport on cooldown for ${outcome.remaining.inWholeSeconds}s"
            TeleportOutcome.AlreadyPending -> "a teleport is already in progress"
            is TeleportOutcome.Cancelled -> return cancellation(outcome.reason)
            TeleportOutcome.PassengerConflict -> "you are riding a vehicle"
            TeleportOutcome.RuntimeStopping -> "runtime is shutting down"
        }
    )

    private fun cancellation(reason: TeleportCancellation): Component? =
        Messages.prefixed(
            when (reason) {
                TeleportCancellation.MOVED -> "teleport cancelled: you moved"
                TeleportCancellation.DAMAGED -> "teleport cancelled: you took damage"
                TeleportCancellation.DIMENSION_CHANGED -> "teleport cancelled: you changed dimension"
                TeleportCancellation.DISCONNECTED,
                TeleportCancellation.SERVER_STOPPING -> return null
            }
        )

}
