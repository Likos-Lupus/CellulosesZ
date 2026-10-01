package top.likoslupus.cellulosesz.movement.teleport.safety

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids

/**
 * Production [TeleportProbe]. Uses the game's own collision shapes, fluid tags, world border, and
 * build height rather than a hand-maintained block blacklist. Every method must run on the server
 * thread; the backend creates one probe per teleport attempt.
 */
internal class MinecraftTeleportProbe(
    private val level: ServerLevel,
    private val player: ServerPlayer,
) : TeleportProbe {

    override fun isWithinBorder(x: Double, z: Double): Boolean =
        level.worldBorder.isWithinBounds(x, z)

    override fun isWithinHeight(y: Double): Boolean =
        y >= level.minY.toDouble() && y < level.maxY.toDouble()

    override fun isChunkLoaded(blockX: Int, blockZ: Int): Boolean =
        level.isLoaded(BlockPos(blockX, level.minY, blockZ))

    override fun classify(x: Double, y: Double, z: Double): SurfaceKind {
        val feet = BlockPos.containing(x, y, z)
        val head = BlockPos.containing(x, y + 1.0, z)

        val feetFluid = level.getFluidState(feet).type
        val headFluid = level.getFluidState(head).type
        if (feetFluid in LAVA_FLUIDS
            || headFluid in LAVA_FLUIDS
        ) {
            return SurfaceKind.LAVA
        }

        val feetBlock = level.getBlockState(feet).block
        val headBlock = level.getBlockState(head).block
        if (feetBlock in DANGEROUS_BLOCKS
            || headBlock in DANGEROUS_BLOCKS
        ) {
            return SurfaceKind.DANGEROUS
        }

        val aabb = player.boundingBox.move(
            x - player.x,
            y - player.y,
            z - player.z
        )
        if (!level.noCollision(player, aabb)) {
            return SurfaceKind.BLOCKED
        }

        val below = feet.below()
        if (level.getBlockState(below).getCollisionShape(level, below).isEmpty) {
            return SurfaceKind.NO_FLOOR
        }

        return if (feetFluid in WATER_FLUIDS
            || headFluid in WATER_FLUIDS
        ) {
            SurfaceKind.WATER
        } else {
            SurfaceKind.SAFE
        }
    }

    private companion object {

        val LAVA_FLUIDS: Set<Fluid> = setOf(Fluids.LAVA, Fluids.FLOWING_LAVA)
        val WATER_FLUIDS: Set<Fluid> = setOf(Fluids.WATER, Fluids.FLOWING_WATER)

        val DANGEROUS_BLOCKS = hashSetOf(
            Blocks.FIRE,
            Blocks.SOUL_FIRE,
            Blocks.MAGMA_BLOCK,
            Blocks.CAMPFIRE,
            Blocks.SOUL_CAMPFIRE,
            Blocks.CACTUS,
        )

    }

}
