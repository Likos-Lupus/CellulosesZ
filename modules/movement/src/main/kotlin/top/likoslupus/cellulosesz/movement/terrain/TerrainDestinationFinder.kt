package top.likoslupus.cellulosesz.movement.terrain

import top.likoslupus.cellulosesz.movement.config.TerrainSettings
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.safety.SurfaceKind
import kotlin.math.floor

/**
 * Deterministic, bounded column lookup shared by `/top`, `/bottom` and `/jump`. It never scans wider
 * than [MAX_VERTICAL_SCAN] blocks from the column's surface or floor, so a pathological column cannot
 * turn into an unbounded world scan.
 */
internal class TerrainDestinationFinder {

    fun surface(
        origin: StoredPosition,
        settings: TerrainSettings,
        probe: TerrainProbe,
    ): TerrainResult {
        val blockX = floor(origin.x).toInt()
        val blockZ = floor(origin.z).toInt()
        if (settings.requireLoadedChunks
            && !probe.isChunkLoaded(blockX, blockZ)
        ) {
            return TerrainResult.ChunkNotLoaded
        }

        val top = probe.highestBlockY(blockX, blockZ)
            ?: return TerrainResult.NoSafeSpot
        return scanDown(probe, settings, origin, top) { position ->
            TerrainResult.Found(position)
        }
    }

    fun bottom(
        origin: StoredPosition,
        settings: TerrainSettings,
        probe: TerrainProbe,
    ): TerrainResult {
        val blockX = floor(origin.x).toInt()
        val blockZ = floor(origin.z).toInt()
        if (settings.requireLoadedChunks && !probe.isChunkLoaded(blockX, blockZ)) {
            return TerrainResult.ChunkNotLoaded
        }

        val low = probe.lowestBlockY(blockX, blockZ)
            ?: return TerrainResult.NoSafeSpot
        for (y in (low + 1)..(low + MAX_VERTICAL_SCAN)) {
            if (standingSpot(probe, settings, origin.x, y.toDouble(), origin.z)) {
                return TerrainResult.Found(origin.copy(y = y.toDouble()))
            }
        }
        return TerrainResult.NoSafeSpot
    }

    fun jump(
        origin: StoredPosition,
        blockX: Int,
        blockZ: Int,
        settings: TerrainSettings,
        probe: TerrainProbe,
    ): TerrainResult {
        if (settings.requireLoadedChunks
            && !probe.isChunkLoaded(blockX, blockZ)
        ) {
            return TerrainResult.ChunkNotLoaded
        }

        val top = probe.highestBlockY(blockX, blockZ)
            ?: return TerrainResult.NoSafeSpot
        val x = blockX + 0.5
        val z = blockZ + 0.5
        return scanDown(probe, settings, origin.copy(x = x, z = z), top) { position ->
            TerrainResult.Found(position)
        }
    }

    private inline fun scanDown(
        probe: TerrainProbe,
        settings: TerrainSettings,
        origin: StoredPosition,
        top: Int,
        found: (StoredPosition) -> TerrainResult,
    ): TerrainResult {
        val from = top + 1
        for (y in from downTo from - MAX_VERTICAL_SCAN) {
            if (standingSpot(probe, settings, origin.x, y.toDouble(), origin.z)) {
                return found(origin.copy(y = y.toDouble()))
            }
        }
        return TerrainResult.NoSafeSpot
    }

    private fun standingSpot(
        probe: TerrainProbe,
        settings: TerrainSettings,
        x: Double,
        y: Double,
        z: Double,
    ): Boolean =
        probe.isWithinHeight(y) &&
                when (probe.classify(x, y, z)) {
                    SurfaceKind.SAFE -> true
                    SurfaceKind.WATER -> settings.allowWater
                    else -> false
                }

    private companion object {

        const val MAX_VERTICAL_SCAN: Int = 16

    }

}
