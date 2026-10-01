package top.likoslupus.cellulosesz.warp

import top.likoslupus.cellulosesz.teleport.TeleportResult
import top.likoslupus.cellulosesz.teleport.TeleportService
import java.util.*

internal sealed interface SetWarpResult {

    data object Success : SetWarpResult

    data object InvalidName : SetWarpResult

    data object PlayerOffline : SetWarpResult

}

internal sealed interface WarpLookupResult {

    data class Found(val warp: Warp) : WarpLookupResult

    data object InvalidName : WarpLookupResult

    data object NotFound : WarpLookupResult

}

internal sealed interface DeleteWarpResult {

    data object Deleted : DeleteWarpResult

    data object InvalidName : DeleteWarpResult

    data object NotFound : DeleteWarpResult

}

internal class WarpService(
    private val repository: WarpRepository,
    private val teleport: TeleportService,
) {

    suspend fun set(playerId: UUID, rawName: String): SetWarpResult {
        val name = WarpName.parse(rawName)
            ?: return SetWarpResult.InvalidName
        val position = teleport.capturePosition(playerId)
            ?: return SetWarpResult.PlayerOffline
        repository.put(Warp(name, position))
        return SetWarpResult.Success
    }

    suspend fun get(rawName: String): WarpLookupResult {
        val name = WarpName.parse(rawName)
            ?: return WarpLookupResult.InvalidName
        val warp = repository.get(name)
            ?: return WarpLookupResult.NotFound
        return WarpLookupResult.Found(warp)
    }

    suspend fun delete(rawName: String): DeleteWarpResult {
        val name = WarpName.parse(rawName)
            ?: return DeleteWarpResult.InvalidName
        return if (repository.remove(name)) {
            DeleteWarpResult.Deleted
        } else {
            DeleteWarpResult.NotFound
        }
    }

    suspend fun list(): List<Warp> = repository.list()

    suspend fun teleportTo(playerId: UUID, warp: Warp): TeleportResult =
        teleport.teleport(playerId, warp.position)

}
