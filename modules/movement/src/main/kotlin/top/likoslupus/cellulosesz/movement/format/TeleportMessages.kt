package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome

internal fun MovementMessages.teleportOutcome(outcome: TeleportOutcome): Notice? =
    notice(
        when (outcome) {
            is TeleportOutcome.Success ->
                return null

            TeleportOutcome.PlayerOffline ->
                MessageKey("movement.teleport.error.player_offline")

            TeleportOutcome.TargetOffline ->
                MessageKey("movement.teleport.error.target_offline")

            is TeleportOutcome.UnknownDimension ->
                return notice(
                    MessageKey("movement.teleport.error.unknown_dimension"),
                    identifier(outcome.dimension)
                )

            is TeleportOutcome.WorldAccessDenied ->
                return notice(
                    MessageKey("movement.teleport.error.world_access"),
                    identifier(outcome.dimension)
                )

            TeleportOutcome.OutsideWorldBorder ->
                MessageKey("movement.teleport.error.outside_border")

            TeleportOutcome.UnsafeDestination ->
                MessageKey("movement.teleport.error.unsafe")

            is TeleportOutcome.Cooldown ->
                return notice(
                    MessageKey("movement.teleport.error.cooldown"),
                    elapsed(outcome.remaining)
                )

            TeleportOutcome.AlreadyPending ->
                MessageKey("movement.teleport.error.already_pending")

            TeleportOutcome.Confined ->
                MessageKey("movement.teleport.error.confined")

            is TeleportOutcome.Cancelled ->
                return cancellation(outcome.reason)

            TeleportOutcome.PassengerConflict ->
                MessageKey("movement.teleport.error.passenger")

            TeleportOutcome.RejectedByPlatform ->
                MessageKey("movement.teleport.error.rejected")

            is TeleportOutcome.RejectedByHook ->
                MessageKey("movement.teleport.error.rejected")

            TeleportOutcome.RuntimeStopping ->
                MessageKey("core.error.runtime_stopping")
        }
    )

internal fun MovementMessages.playerOffline(): Notice =
    notice(MessageKey("movement.teleport.error.player_offline"))

internal fun MovementMessages.teleportComplete(): Notice =
    success(MessageKey("movement.teleport.complete"))

internal fun MovementMessages.teleportBack(): Notice =
    success(MessageKey("movement.teleport.back"))

internal fun MovementMessages.noPreviousLocation(): Notice =
    notice(MessageKey("movement.teleport.no_previous"))

private fun MovementMessages.cancellation(reason: TeleportCancellation): Notice? =
    notice(
        when (reason) {
            TeleportCancellation.MOVED ->
                MessageKey("movement.teleport.cancelled.moved")

            TeleportCancellation.DAMAGED ->
                MessageKey("movement.teleport.cancelled.damaged")

            TeleportCancellation.DIMENSION_CHANGED ->
                MessageKey("movement.teleport.cancelled.dimension")

            TeleportCancellation.DISCONNECTED,
            TeleportCancellation.SERVER_STOPPING -> return null
        }
    )
