package top.likoslupus.cellulosesz.movement.terrain

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

/** Which terrain lookup a command asked for. */
internal enum class TerrainTarget {

    TOP,
    BOTTOM,
    JUMP,

}

/** Expected outcome of a terrain lookup; failures are values, not exceptions. */
internal sealed interface TerrainResult {

    data class Found(val position: StoredPosition) : TerrainResult

    data object NoSafeSpot : TerrainResult

    data object ChunkNotLoaded : TerrainResult

    data object NoTarget : TerrainResult

    data object PlayerOffline : TerrainResult

    data object Disabled : TerrainResult

}
