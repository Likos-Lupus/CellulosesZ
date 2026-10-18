package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*
import kotlin.time.toJavaDuration

internal fun TeleportTarget.toMovementTarget(): MovementTravelTarget =
    when (this) {
        is TeleportTarget.Home -> MovementTravelTarget.Home(name)
        is TeleportTarget.Warp -> MovementTravelTarget.Warp(name)
        TeleportTarget.Spawn -> MovementTravelTarget.Spawn
        is TeleportTarget.Player -> MovementTravelTarget.Player(playerId)
        is TeleportTarget.World -> MovementTravelTarget.World(worldId)
    }

private fun MovementPosition.toApi(): ApiPosition =
    ApiPosition(dimension, x, y, z, yaw, pitch)

private fun MovementHome.toView(): HomeView =
    HomeView(ownerId, name, position.toApi())

private fun MovementWarp.toView(): WarpView =
    WarpView(name, position?.toApi(), createdBy, updatedAt)

internal fun MovementHomeListResult.toApi(): HomeListOutcome =
    when (this) {
        is MovementHomeListResult.Listed -> HomeListOutcome.Listed(homes.map { it.toView() })
        MovementHomeListResult.StorageUnavailable -> HomeListOutcome.StorageUnavailable
    }

internal fun MovementHomeLookupResult.toApi(): HomeLookupOutcome =
    when (this) {
        is MovementHomeLookupResult.Found -> HomeLookupOutcome.Found(home.toView())
        MovementHomeLookupResult.NotFound -> HomeLookupOutcome.NotFound
        MovementHomeLookupResult.InvalidName -> HomeLookupOutcome.InvalidName
        MovementHomeLookupResult.StorageUnavailable -> HomeLookupOutcome.StorageUnavailable
    }

internal fun MovementHomeMutationResult.toApi(): HomeMutationOutcome =
    when (this) {
        is MovementHomeMutationResult.Created -> HomeMutationOutcome.Created(name)
        is MovementHomeMutationResult.Updated -> HomeMutationOutcome.Updated(name)
        is MovementHomeMutationResult.Renamed -> HomeMutationOutcome.Renamed(name)
        is MovementHomeMutationResult.Deleted -> HomeMutationOutcome.Deleted(name)
        MovementHomeMutationResult.InvalidName -> HomeMutationOutcome.InvalidName
        MovementHomeMutationResult.NotFound -> HomeMutationOutcome.NotFound
        MovementHomeMutationResult.AlreadyExists -> HomeMutationOutcome.AlreadyExists
        MovementHomeMutationResult.LimitReached -> HomeMutationOutcome.LimitReached
        MovementHomeMutationResult.PlayerOffline -> HomeMutationOutcome.PlayerOffline
        MovementHomeMutationResult.StorageUnavailable -> HomeMutationOutcome.StorageUnavailable
    }

internal fun MovementWarpListResult.toApi(): WarpListOutcome =
    when (this) {
        is MovementWarpListResult.Listed -> WarpListOutcome.Listed(warps.map { it.toView() })
        MovementWarpListResult.NotAuthorized -> WarpListOutcome.NotAuthorized
        MovementWarpListResult.StorageUnavailable -> WarpListOutcome.StorageUnavailable
    }

internal fun MovementWarpLookupResult.toApi(): WarpLookupOutcome =
    when (this) {
        is MovementWarpLookupResult.Found -> WarpLookupOutcome.Found(warp.toView())
        MovementWarpLookupResult.NotFound -> WarpLookupOutcome.NotFound
        MovementWarpLookupResult.InvalidName -> WarpLookupOutcome.InvalidName
        MovementWarpLookupResult.NotAuthorized -> WarpLookupOutcome.NotAuthorized
        MovementWarpLookupResult.StorageUnavailable -> WarpLookupOutcome.StorageUnavailable
    }

internal fun MovementWarpMutationResult.toApi(): WarpMutationOutcome =
    when (this) {
        is MovementWarpMutationResult.Created -> WarpMutationOutcome.Created(name)
        is MovementWarpMutationResult.Deleted -> WarpMutationOutcome.Deleted(name)
        MovementWarpMutationResult.NotFound -> WarpMutationOutcome.NotFound
        MovementWarpMutationResult.InvalidName -> WarpMutationOutcome.InvalidName
        MovementWarpMutationResult.NotAuthorized -> WarpMutationOutcome.NotAuthorized
        MovementWarpMutationResult.PlayerOffline -> WarpMutationOutcome.PlayerOffline
        MovementWarpMutationResult.StorageUnavailable -> WarpMutationOutcome.StorageUnavailable
    }

internal fun MovementSpawnQueryResult.toApi(): SpawnQueryOutcome =
    when (this) {
        is MovementSpawnQueryResult.Configured -> SpawnQueryOutcome.Configured(position.toApi())
        MovementSpawnQueryResult.NotConfigured -> SpawnQueryOutcome.NotConfigured
        MovementSpawnQueryResult.NotAuthorized -> SpawnQueryOutcome.NotAuthorized
        MovementSpawnQueryResult.StorageUnavailable -> SpawnQueryOutcome.StorageUnavailable
    }

internal fun MovementSpawnMutationResult.toApi(): SpawnMutationOutcome =
    when (this) {
        MovementSpawnMutationResult.Updated -> SpawnMutationOutcome.Updated
        MovementSpawnMutationResult.Cleared -> SpawnMutationOutcome.Cleared
        MovementSpawnMutationResult.NotAuthorized -> SpawnMutationOutcome.NotAuthorized
        MovementSpawnMutationResult.PlayerOffline -> SpawnMutationOutcome.PlayerOffline
        MovementSpawnMutationResult.StorageUnavailable -> SpawnMutationOutcome.StorageUnavailable
    }

internal fun MovementTravelResult.toApi(): TeleportActionOutcome =
    when (this) {
        is MovementTravelResult.Committed -> TeleportActionOutcome.Committed(
            TeleportReceiptView(
                playerId = receipt.playerId,
                from = receipt.from.toApi(),
                to = receipt.to.toApi(),
                origin = receipt.origin.toApiKind(),
            )
        )

        MovementTravelResult.PlayerOffline -> TeleportActionOutcome.PlayerOffline
        MovementTravelResult.TargetOffline -> TeleportActionOutcome.TargetOffline
        MovementTravelResult.DestinationNotFound -> TeleportActionOutcome.DestinationNotFound
        MovementTravelResult.Denied -> TeleportActionOutcome.Denied
        MovementTravelResult.Confined -> TeleportActionOutcome.Confined
        MovementTravelResult.UnsafeDestination -> TeleportActionOutcome.UnsafeDestination
        MovementTravelResult.PlatformRejected -> TeleportActionOutcome.PlatformRejected
        is MovementTravelResult.Cooldown -> TeleportActionOutcome.Cooldown(remaining.toJavaDuration())
        MovementTravelResult.AlreadyPending -> TeleportActionOutcome.AlreadyPending
        MovementTravelResult.Cancelled -> TeleportActionOutcome.Cancelled
        MovementTravelResult.OtherFailure -> TeleportActionOutcome.OtherFailure
    }

internal fun MovementRequestActionResult.toApi(): TeleportRequestActionOutcome =
    when (this) {
        is MovementRequestActionResult.Created -> TeleportRequestActionOutcome.Created(requestId)
        MovementRequestActionResult.Accepted -> TeleportRequestActionOutcome.Accepted
        MovementRequestActionResult.Denied -> TeleportRequestActionOutcome.Denied
        MovementRequestActionResult.Cancelled -> TeleportRequestActionOutcome.Cancelled
        MovementRequestActionResult.Expired -> TeleportRequestActionOutcome.Expired
        MovementRequestActionResult.TargetUnavailable -> TeleportRequestActionOutcome.TargetUnavailable
        MovementRequestActionResult.AlreadyPending -> TeleportRequestActionOutcome.AlreadyPending
        MovementRequestActionResult.NotAuthorized -> TeleportRequestActionOutcome.NotAuthorized
        MovementRequestActionResult.RequestLimit -> TeleportRequestActionOutcome.RequestLimit
        MovementRequestActionResult.OtherFailure -> TeleportRequestActionOutcome.OtherFailure
    }
