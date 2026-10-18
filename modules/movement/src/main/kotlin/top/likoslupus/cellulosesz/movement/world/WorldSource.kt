package top.likoslupus.cellulosesz.movement.world

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

/**
 * Server-side world facts the `/world` command needs. Kept behind an interface so alias resolution
 * and access policy stay pure and testable; the Minecraft implementation is the only file that
 * touches loaded levels.
 */
internal interface WorldSource {

    suspend fun availableDimensions(): List<String>

    /** The dimension's spawn, or null when the dimension is not loaded. */
    suspend fun spawnPosition(dimension: String): StoredPosition?

}

internal class MinecraftWorldSource(private val kernel: RuntimeKernel) : WorldSource {

    override suspend fun availableDimensions(): List<String> =
        kernel.onServerThread {
            kernel.requireServer().allLevels.map { it.dimension().identifier().toString() }
        }

    override suspend fun spawnPosition(dimension: String): StoredPosition? =
        kernel.onServerThread {
            val identifier = Identifier.tryParse(dimension)
                ?: return@onServerThread null
            val key = ResourceKey.create(Registries.DIMENSION, identifier)
            val level = kernel.requireServer().getLevel(key)
                ?: return@onServerThread null
            val spawn = level.respawnData
            StoredPosition(
                dimension = spawn.dimension().identifier().toString(),
                x = spawn.pos().x + 0.5,
                y = spawn.pos().y.toDouble(),
                z = spawn.pos().z + 0.5,
                yaw = spawn.yaw(),
                pitch = spawn.pitch(),
            )
        }

}
