package top.likoslupus.cellulosesz.movement.integration

import top.likoslupus.cellulosesz.movement.home.DeleteHomeResult
import top.likoslupus.cellulosesz.movement.home.HomeDataException
import top.likoslupus.cellulosesz.movement.home.HomeLookupResult
import top.likoslupus.cellulosesz.movement.home.HomeService
import top.likoslupus.cellulosesz.movement.home.RenameHomeResult
import top.likoslupus.cellulosesz.movement.home.SetHomeResult
import java.util.UUID

/** Owned-home reads and mutations behind [MovementIntegrationPort]. */
internal class MovementHomeIntegration(
    private val homes: HomeService,
    private val context: MovementIntegrationContext,
) {

    suspend fun listHomes(ownerId: UUID): MovementHomeListResult =
        try {
            MovementHomeListResult.Listed(homes.list(ownerId).map { it.toMovement(ownerId) })
        } catch (_: HomeDataException) {
            MovementHomeListResult.StorageUnavailable
        }

    suspend fun findHome(ownerId: UUID, name: String?): MovementHomeLookupResult =
        try {
            when (val result = homes.get(ownerId, name)) {
                is HomeLookupResult.Found ->
                    MovementHomeLookupResult.Found(result.home.toMovement(ownerId))

                HomeLookupResult.InvalidName ->
                    MovementHomeLookupResult.InvalidName

                HomeLookupResult.NotFound ->
                    MovementHomeLookupResult.NotFound
            }
        } catch (_: HomeDataException) {
            MovementHomeLookupResult.StorageUnavailable
        }

    suspend fun setHomeAtCurrentPosition(
        actorId: UUID,
        name: String?,
    ): MovementHomeMutationResult {
        return try {
            val position = context.backend.position(actorId)
                ?: return MovementHomeMutationResult.PlayerOffline
            val resolvedName = name ?: context.settings().homes.defaultName
            val existed = homes.get(actorId, resolvedName) is HomeLookupResult.Found
            when (homes.set(actorId, name, position)) {
                SetHomeResult.Success -> {
                    if (existed) {
                        context.notifyHomeChanged(
                            actorId,
                            resolvedName,
                            MovementHomeChangeKind.UPDATED
                        )
                        MovementHomeMutationResult.Updated(resolvedName)
                    } else {
                        context.notifyHomeChanged(
                            actorId,
                            resolvedName,
                            MovementHomeChangeKind.CREATED
                        )
                        MovementHomeMutationResult.Created(resolvedName)
                    }
                }

                SetHomeResult.InvalidName -> MovementHomeMutationResult.InvalidName
                SetHomeResult.LimitReached -> MovementHomeMutationResult.LimitReached
            }
        } catch (_: HomeDataException) {
            MovementHomeMutationResult.StorageUnavailable
        }
    }

    suspend fun renameHome(
        actorId: UUID,
        oldName: String,
        newName: String,
    ): MovementHomeMutationResult =
        try {
            when (homes.rename(actorId, oldName, newName)) {
                RenameHomeResult.Renamed -> {
                    context.notifyHomeChanged(
                        actorId,
                        newName,
                        MovementHomeChangeKind.RENAMED
                    )
                    MovementHomeMutationResult.Renamed(newName)
                }

                RenameHomeResult.InvalidName -> MovementHomeMutationResult.InvalidName
                RenameHomeResult.SourceNotFound -> MovementHomeMutationResult.NotFound
                RenameHomeResult.DestinationExists -> MovementHomeMutationResult.AlreadyExists
            }
        } catch (_: HomeDataException) {
            MovementHomeMutationResult.StorageUnavailable
        }

    suspend fun deleteHome(actorId: UUID, name: String): MovementHomeMutationResult =
        try {
            when (homes.delete(actorId, name)) {
                DeleteHomeResult.Deleted -> {
                    context.notifyHomeChanged(
                        actorId,
                        name,
                        MovementHomeChangeKind.DELETED
                    )
                    MovementHomeMutationResult.Deleted(name)
                }

                DeleteHomeResult.InvalidName -> MovementHomeMutationResult.InvalidName
                DeleteHomeResult.NotFound -> MovementHomeMutationResult.NotFound
            }
        } catch (_: HomeDataException) {
            MovementHomeMutationResult.StorageUnavailable
        }

}
