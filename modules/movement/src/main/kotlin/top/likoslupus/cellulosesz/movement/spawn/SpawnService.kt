package top.likoslupus.cellulosesz.movement.spawn

import top.likoslupus.cellulosesz.movement.teleport.TeleportResult
import top.likoslupus.cellulosesz.movement.teleport.TeleportService
import java.util.*

internal sealed interface SetSpawnResult {

    data object Success : SetSpawnResult

    data object PlayerOffline : SetSpawnResult

}

internal sealed interface ClearSpawnResult {

    data object Cleared : ClearSpawnResult

    data object NotConfigured : ClearSpawnResult

}

internal class SpawnService(
    private val repository: SpawnRepository,
    private val teleport: TeleportService,
) {

    suspend fun set(playerId: UUID): SetSpawnResult {
        val position = teleport.capturePosition(playerId)
            ?: return SetSpawnResult.PlayerOffline
        repository.write(position)
        return SetSpawnResult.Success
    }

    suspend fun clear(): ClearSpawnResult =
        if (repository.read() == null) {
            ClearSpawnResult.NotConfigured
        } else {
            repository.write(null)
            ClearSpawnResult.Cleared
        }

    suspend fun teleportToSpawn(playerId: UUID): TeleportResult {
        val configured = repository.read()
        return teleport.teleport(
            playerId,
            configured ?: teleport.vanillaSpawnPosition()
        )
    }

}
