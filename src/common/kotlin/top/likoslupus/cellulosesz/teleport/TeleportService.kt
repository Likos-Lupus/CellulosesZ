package top.likoslupus.cellulosesz.teleport

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.portal.TeleportTransition
import net.minecraft.world.phys.Vec3
import top.likoslupus.cellulosesz.player.PlayerResolver
import top.likoslupus.cellulosesz.runtime.RuntimeKernel
import java.util.*

internal sealed interface TeleportResult {

    data object Success : TeleportResult

    data object PlayerOffline : TeleportResult

    data class UnknownDimension(val dimension: String) : TeleportResult

}

internal class TeleportService(private val kernel: RuntimeKernel) {

    suspend fun capturePosition(playerId: UUID): StoredPosition? =
        kernel.onServerThread {
            val player = PlayerResolver.onlineById(kernel.requireServer(), playerId)
                ?: return@onServerThread null
            StoredPosition(
                dimension = player.level().dimension().identifier().toString(),
                x = player.x,
                y = player.y,
                z = player.z,
                yaw = player.yRot,
                pitch = player.xRot,
            )
        }

    suspend fun teleport(playerId: UUID, position: StoredPosition): TeleportResult =
        kernel.onServerThread {
            val server = kernel.requireServer()
            val player = PlayerResolver.onlineById(server, playerId)
                ?: return@onServerThread TeleportResult.PlayerOffline

            val key = dimensionKey(position.dimension)
                ?: return@onServerThread TeleportResult.UnknownDimension(position.dimension)
            val level = server.getLevel(key)
                ?: return@onServerThread TeleportResult.UnknownDimension(position.dimension)

            player.teleport(
                TeleportTransition(
                    level,
                    Vec3(position.x, position.y, position.z),
                    Vec3.ZERO,
                    position.yaw,
                    position.pitch,
                    TeleportTransition.DO_NOTHING,
                )
            )
            TeleportResult.Success
        }

    suspend fun vanillaSpawnPosition(): StoredPosition =
        kernel.onServerThread {
            val server = kernel.requireServer()
            val spawn = server.overworld().respawnData
            StoredPosition(
                dimension = spawn.dimension().identifier().toString(),
                x = spawn.pos().x + 0.5,
                y = spawn.pos().y.toDouble(),
                z = spawn.pos().z + 0.5,
                yaw = spawn.yaw(),
                pitch = spawn.pitch(),
            )
        }

    private fun dimensionKey(dimension: String): ResourceKey<Level>? {
        val identifier = Identifier.tryParse(dimension)
            ?: return null
        return ResourceKey.create(Registries.DIMENSION, identifier)
    }

}
