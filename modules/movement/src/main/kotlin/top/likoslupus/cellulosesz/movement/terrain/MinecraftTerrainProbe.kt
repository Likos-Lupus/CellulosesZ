package top.likoslupus.cellulosesz.movement.terrain

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.levelgen.Heightmap
import top.likoslupus.cellulosesz.movement.teleport.safety.MinecraftTeleportProbe
import top.likoslupus.cellulosesz.movement.teleport.safety.SurfaceKind

/**
 * Production [TerrainProbe]. Surface height comes from the level's heightmap; the floor scan and
 * standing-space classification reuse the same collision/fluid rules as the teleport safety probe.
 * Every method must run on the server thread.
 */
internal class MinecraftTerrainProbe(
    private val level: ServerLevel,
    private val delegate: MinecraftTeleportProbe,
) : TerrainProbe {

    override fun isChunkLoaded(blockX: Int, blockZ: Int): Boolean =
        level.isLoaded(BlockPos(blockX, level.minY, blockZ))

    override fun isWithinHeight(y: Double): Boolean =
        y >= level.minY.toDouble() && y < level.maxY.toDouble()

    override fun highestBlockY(blockX: Int, blockZ: Int): Int? {
        val height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ)
        return if (height <= level.minY) null else height - 1
    }

    override fun lowestBlockY(blockX: Int, blockZ: Int): Int? {
        for (y in level.minY until level.maxY) {
            if (!level.getBlockState(BlockPos(blockX, y, blockZ)).isAir) {
                return y
            }
        }
        return null
    }

    override fun classify(x: Double, y: Double, z: Double): SurfaceKind =
        delegate.classify(x, y, z)

    companion object {

        fun forPlayer(level: ServerLevel, player: ServerPlayer): MinecraftTerrainProbe =
            MinecraftTerrainProbe(level, MinecraftTeleportProbe(level, player))

    }

}
