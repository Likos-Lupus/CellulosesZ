package top.likoslupus.cellulosesz.movement.spawn

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

internal sealed interface ClearSpawnResult {

    data object Cleared : ClearSpawnResult

    data object NotConfigured : ClearSpawnResult

}

/** Owns the configured spawn; resolving vanilla spawn and moving players happen above it. */
internal class SpawnService(private val repository: SpawnRepository) {

    suspend fun set(position: StoredPosition) =
        repository.write(position)

    suspend fun clear(): ClearSpawnResult =
        if (repository.read() == null) {
            ClearSpawnResult.NotConfigured
        } else {
            repository.write(null)
            ClearSpawnResult.Cleared
        }

    suspend fun configured(): StoredPosition? =
        repository.read()

}
