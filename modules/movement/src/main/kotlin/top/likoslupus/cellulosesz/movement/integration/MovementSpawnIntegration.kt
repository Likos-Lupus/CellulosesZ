package top.likoslupus.cellulosesz.movement.integration

import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.movement.location.LastLocationService
import top.likoslupus.cellulosesz.movement.spawn.ClearSpawnResult
import top.likoslupus.cellulosesz.movement.spawn.SpawnDataException
import top.likoslupus.cellulosesz.movement.spawn.SpawnService
import java.util.*

/** The world spawn and the persisted last-known locations behind [MovementIntegrationPort]. */
internal class MovementSpawnIntegration(
    private val spawn: SpawnService,
    private val lastLocations: LastLocationService,
    private val context: MovementIntegrationContext,
) {

    suspend fun lastKnownLocation(playerId: UUID): MovementLastLocationResult =
        when (val cached = lastLocations.lastLocation(playerId)) {
            null -> MovementLastLocationResult.Unknown
            else -> MovementLastLocationResult.Found(cached.position.toMovement())
        }

    suspend fun configuredSpawn(viewerId: UUID): MovementSpawnQueryResult {
        if (
            context.verdict(
                viewerId,
                CommandPermissions.SPAWN
            ) != Verdict.ALLOWED
        ) {
            return MovementSpawnQueryResult.NotAuthorized
        }
        return try {
            spawn.configured()?.let {
                MovementSpawnQueryResult.Configured(it.toMovement())
            } ?: MovementSpawnQueryResult.NotConfigured
        } catch (_: SpawnDataException) {
            MovementSpawnQueryResult.StorageUnavailable
        }
    }

    suspend fun setSpawnFromCurrent(actorId: UUID): MovementSpawnMutationResult {
        when (context.verdict(actorId, CommandPermissions.SET_SPAWN)) {
            Verdict.DENIED -> return MovementSpawnMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementSpawnMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val position = context.backend.position(actorId)
            ?: return MovementSpawnMutationResult.PlayerOffline
        return try {
            spawn.set(position)
            context.notifySpawnChanged(true)
            MovementSpawnMutationResult.Updated
        } catch (_: SpawnDataException) {
            MovementSpawnMutationResult.StorageUnavailable
        }
    }

    suspend fun clearSpawn(actorId: UUID): MovementSpawnMutationResult {
        when (context.verdict(actorId, CommandPermissions.DEL_SPAWN)) {
            Verdict.DENIED -> return MovementSpawnMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementSpawnMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        return try {
            when (spawn.clear()) {
                ClearSpawnResult.Cleared -> {
                    context.notifySpawnChanged(false)
                    MovementSpawnMutationResult.Cleared
                }

                // Nothing was configured: the end state ("no spawn") already holds, and no change is
                // observed. The public contract has no distinct "not configured" branch.
                ClearSpawnResult.NotConfigured -> MovementSpawnMutationResult.Cleared
            }
        } catch (_: SpawnDataException) {
            MovementSpawnMutationResult.StorageUnavailable
        }
    }

}
