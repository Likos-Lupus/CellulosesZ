package top.likoslupus.cellulosesz.movement.integration

import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.movement.warp.*
import java.time.Instant
import java.util.*

/** Warp reads and mutations behind [MovementIntegrationPort]. */
internal class MovementWarpIntegration(
    private val warps: WarpService,
    private val context: MovementIntegrationContext,
) {

    suspend fun listWarps(viewerId: UUID): MovementWarpListResult {
        if (context.verdict(viewerId, CommandPermissions.WARPS) != Verdict.ALLOWED) {
            return MovementWarpListResult.NotAuthorized
        }
        val showCoordinates = context.verdict(
            viewerId,
            CommandPermissions.WARP_INFO_COORDINATES
        ) == Verdict.ALLOWED
        return try {
            MovementWarpListResult.Listed(
                context.access.visibleWarps(viewerId, warps.list()).map {
                    it.toMovement(showCoordinates, null)
                }
            )
        } catch (_: WarpDataException) {
            MovementWarpListResult.StorageUnavailable
        }
    }

    suspend fun findWarp(viewerId: UUID, name: String): MovementWarpLookupResult {
        if (
            context.verdict(
                viewerId,
                CommandPermissions.WARP
            ) != Verdict.ALLOWED
        ) {
            return MovementWarpLookupResult.NotAuthorized
        }

        val showCoordinates = context.verdict(
            viewerId,
            CommandPermissions.WARP_INFO_COORDINATES
        ) == Verdict.ALLOWED
        return try {
            when (val result = warps.info(name)) {
                is WarpInfoResult.Found ->
                    if (!context.access.allowsWarp(viewerId, result.warp.name.value)) {
                        MovementWarpLookupResult.NotAuthorized
                    } else {
                        MovementWarpLookupResult.Found(
                            result.warp.toMovement(
                                showCoordinates,
                                result.metadata
                            )
                        )
                    }

                WarpInfoResult.InvalidName ->
                    MovementWarpLookupResult.InvalidName

                WarpInfoResult.NotFound ->
                    MovementWarpLookupResult.NotFound
            }
        } catch (_: WarpDataException) {
            MovementWarpLookupResult.StorageUnavailable
        }
    }

    suspend fun createWarp(actorId: UUID, name: String): MovementWarpMutationResult {
        when (context.verdict(actorId, CommandPermissions.SET_WARP)) {
            Verdict.DENIED -> return MovementWarpMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementWarpMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        val position = context.backend.position(actorId)
            ?: return MovementWarpMutationResult.PlayerOffline
        val actorName = context.onlineName(actorId)
            ?: return MovementWarpMutationResult.PlayerOffline
        return try {
            when (warps.set(name, position, actorName, Instant.now())) {
                SetWarpResult.Success -> {
                    context.notifyWarpChanged(name, MovementWarpChangeKind.CREATED)
                    MovementWarpMutationResult.Created(name)
                }

                SetWarpResult.InvalidName -> MovementWarpMutationResult.InvalidName
            }
        } catch (_: WarpDataException) {
            MovementWarpMutationResult.StorageUnavailable
        }
    }

    suspend fun deleteWarp(actorId: UUID, name: String): MovementWarpMutationResult {
        when (context.verdict(actorId, CommandPermissions.DEL_WARP)) {
            Verdict.DENIED -> return MovementWarpMutationResult.NotAuthorized
            Verdict.OFFLINE -> return MovementWarpMutationResult.PlayerOffline
            Verdict.ALLOWED -> Unit
        }
        return try {
            when (warps.delete(name)) {
                DeleteWarpResult.Deleted -> {
                    context.notifyWarpChanged(name, MovementWarpChangeKind.DELETED)
                    MovementWarpMutationResult.Deleted(name)
                }

                DeleteWarpResult.InvalidName -> MovementWarpMutationResult.InvalidName
                DeleteWarpResult.NotFound -> MovementWarpMutationResult.NotFound
            }
        } catch (_: WarpDataException) {
            MovementWarpMutationResult.StorageUnavailable
        }
    }

}
