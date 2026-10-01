package top.likoslupus.cellulosesz.movement.warp

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

internal sealed interface SetWarpResult {

    data object Success : SetWarpResult

    data object InvalidName : SetWarpResult

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

/** Owns global warp storage and naming policy only; it never moves a player. */
internal class WarpService(private val repository: WarpRepository) {

    suspend fun set(rawName: String, position: StoredPosition): SetWarpResult {
        val name = WarpName.parse(rawName)
            ?: return SetWarpResult.InvalidName
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

}
