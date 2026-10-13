package top.likoslupus.cellulosesz.movement.terrain

import top.likoslupus.cellulosesz.movement.teleport.safety.SurfaceKind

/**
 * Read-only column view used by [TerrainDestinationFinder]. It exists as an interface so the finder's
 * ordering and bounds logic can be unit-tested without a running Minecraft server; the production
 * implementation is the only place that touches `ServerLevel`.
 */
internal interface TerrainProbe {

    fun isChunkLoaded(blockX: Int, blockZ: Int): Boolean

    fun isWithinHeight(y: Double): Boolean

    /** Highest non-air block in the column, or null when the column is empty. */
    fun highestBlockY(blockX: Int, blockZ: Int): Int?

    /** Lowest non-air block in the column, or null when the column is empty. */
    fun lowestBlockY(blockX: Int, blockZ: Int): Int?

    fun classify(x: Double, y: Double, z: Double): SurfaceKind

}
