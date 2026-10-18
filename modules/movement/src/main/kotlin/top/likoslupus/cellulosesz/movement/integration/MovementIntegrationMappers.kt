package top.likoslupus.cellulosesz.movement.integration

import top.likoslupus.cellulosesz.movement.home.Home
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome
import top.likoslupus.cellulosesz.movement.teleport.toOrigin
import top.likoslupus.cellulosesz.movement.warp.Warp
import top.likoslupus.cellulosesz.movement.warp.WarpMetadata
import java.util.*

/**
 * Translations between the Movement domain model and the integration contract. Kept in one place so
 * the storage/teleport types never leak past [MovementIntegrationPort].
 */

internal fun StoredPosition.toMovement(): MovementPosition =
    MovementPosition(dimension, x, y, z, yaw, pitch)

internal fun Home.toMovement(ownerId: UUID): MovementHome =
    MovementHome(ownerId, name.value, position.toMovement())

internal fun Warp.toMovement(showCoordinates: Boolean, metadata: WarpMetadata?): MovementWarp =
    MovementWarp(
        name = name.value,
        position = if (showCoordinates) position.toMovement() else null,
        createdBy = metadata?.createdBy,
        updatedAt = metadata?.updatedAt,
    )

internal fun TeleportOutcome.toMovementTravelResult(): MovementTravelResult =
    when (this) {
        is TeleportOutcome.Success -> MovementTravelResult.Committed(
            MovementTeleportReceipt(
                playerId = receipt.subjectId,
                from = receipt.origin.toMovement(),
                to = receipt.destination.toMovement(),
                origin = receipt.cause.toOrigin(),
            )
        )

        TeleportOutcome.PlayerOffline -> MovementTravelResult.PlayerOffline
        TeleportOutcome.TargetOffline -> MovementTravelResult.TargetOffline
        // A stored dimension that no longer exists is an internal inconsistency, not a resolvable
        // destination miss: fold it into the generic failure.
        is TeleportOutcome.UnknownDimension -> MovementTravelResult.OtherFailure
        is TeleportOutcome.WorldAccessDenied -> MovementTravelResult.Denied
        TeleportOutcome.OutsideWorldBorder -> MovementTravelResult.OtherFailure
        TeleportOutcome.UnsafeDestination -> MovementTravelResult.UnsafeDestination
        is TeleportOutcome.Cooldown -> MovementTravelResult.Cooldown(remaining)
        TeleportOutcome.AlreadyPending -> MovementTravelResult.AlreadyPending
        TeleportOutcome.Confined -> MovementTravelResult.Confined
        is TeleportOutcome.Cancelled -> MovementTravelResult.Cancelled
        TeleportOutcome.PassengerConflict -> MovementTravelResult.OtherFailure
        TeleportOutcome.RejectedByPlatform -> MovementTravelResult.PlatformRejected
        is TeleportOutcome.RejectedByHook -> MovementTravelResult.Denied
        TeleportOutcome.RuntimeStopping -> MovementTravelResult.OtherFailure
    }

/** The wire form of a pending request: one sender/target pair per entry. */
internal fun requestId(senderId: UUID, targetId: UUID): String =
    "$senderId:$targetId"

internal fun parseRequestId(raw: String): Pair<UUID, UUID>? {
    val separator = raw.indexOf(':')
    if (separator <= 0 || separator == raw.length - 1) {
        return null
    }
    val sender = runCatching {
        UUID.fromString(raw.substring(0, separator))
    }.getOrNull() ?: return null
    val target = runCatching {
        UUID.fromString(raw.substring(separator + 1))
    }.getOrNull() ?: return null
    return sender to target
}
