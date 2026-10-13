package top.likoslupus.cellulosesz.movement.warp

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.time.Instant

internal sealed interface SetWarpResult {

    data object Success : SetWarpResult

    data object InvalidName : SetWarpResult

}

internal sealed interface WarpLookupResult {

    data class Found(val warp: Warp) : WarpLookupResult

    data object InvalidName : WarpLookupResult

    data object NotFound : WarpLookupResult

}

internal sealed interface WarpInfoResult {

    data class Found(val warp: Warp, val metadata: WarpMetadata?) : WarpInfoResult

    data object InvalidName : WarpInfoResult

    data object NotFound : WarpInfoResult

}

internal sealed interface DeleteWarpResult {

    data object Deleted : DeleteWarpResult

    data object InvalidName : DeleteWarpResult

    data object NotFound : DeleteWarpResult

}

/** Owns global warp storage, its sidecar metadata and naming policy only; it never moves a player. */
internal class WarpService(private val repository: WarpRepository) {

    suspend fun set(
        rawName: String,
        position: StoredPosition,
        actor: String,
        now: Instant,
    ): SetWarpResult {
        val name = WarpName.parse(rawName)
            ?: return SetWarpResult.InvalidName
        repository.put(
            Warp(name, position),
            WarpMetadata(actor, now, now)
        )
        return SetWarpResult.Success
    }

    suspend fun get(rawName: String): WarpLookupResult {
        val name = WarpName.parse(rawName)
            ?: return WarpLookupResult.InvalidName
        val warp = repository.get(name)
            ?: return WarpLookupResult.NotFound
        return WarpLookupResult.Found(warp)
    }

    suspend fun info(rawName: String): WarpInfoResult {
        val name = WarpName.parse(rawName)
            ?: return WarpInfoResult.InvalidName
        val warp = repository.get(name)
            ?: return WarpInfoResult.NotFound
        return WarpInfoResult.Found(
            warp,
            repository.metadata(name)
        )
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

    suspend fun list(): List<Warp> =
        repository.list()

}
