package top.likoslupus.cellulosesz.spawn

import top.likoslupus.cellulosesz.teleport.TeleportResult
import top.likoslupus.cellulosesz.teleport.TeleportService
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
        if (configured != null) {
            return teleport.teleport(playerId, configured)
        }
        return teleport.teleport(playerId, teleport.vanillaSpawnPosition())
    }

}
