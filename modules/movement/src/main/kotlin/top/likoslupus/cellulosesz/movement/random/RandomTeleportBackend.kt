package top.likoslupus.cellulosesz.movement.random

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.config.RandomTeleportSettings
import top.likoslupus.cellulosesz.movement.config.TeleportSafetySettings
import top.likoslupus.cellulosesz.movement.config.TerrainSettings
import top.likoslupus.cellulosesz.movement.teleport.SafetyMode
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.safety.MinecraftTeleportProbe
import top.likoslupus.cellulosesz.movement.teleport.safety.SafeDestinationResolver
import top.likoslupus.cellulosesz.movement.teleport.safety.SafetyResult
import top.likoslupus.cellulosesz.movement.terrain.MinecraftTerrainProbe
import top.likoslupus.cellulosesz.movement.terrain.TerrainDestinationFinder
import top.likoslupus.cellulosesz.movement.terrain.TerrainResult
import java.util.*
import kotlin.math.floor

/** Outcome of resolving one random candidate, distinguishing "try another" from "no area at all". */
internal sealed interface RandomResolution {

    data class Safe(val position: StoredPosition) : RandomResolution

    data object Unsafe : RandomResolution

    data object NoValidArea : RandomResolution

}

/** Server-thread adapter that turns a random candidate into a safe standing position, or a failure. */
internal interface RandomTeleportBackend {

    suspend fun resolveSafe(
        playerId: UUID,
        region: RandomTeleportRegion,
        candidate: RandomCandidate,
        settings: RandomTeleportSettings,
    ): RandomResolution

}

internal class MinecraftRandomTeleportBackend(
    private val kernel: RuntimeKernel,
    private val finder: TerrainDestinationFinder,
    private val safety: SafeDestinationResolver,
) : RandomTeleportBackend {

    override suspend fun resolveSafe(
        playerId: UUID,
        region: RandomTeleportRegion,
        candidate: RandomCandidate,
        settings: RandomTeleportSettings,
    ): RandomResolution =
        kernel.onServerThread {
            val server = kernel.requireServer()
            val player = PlayerResolver.onlineById(server, playerId)
                ?: return@onServerThread RandomResolution.Unsafe
            val level = resolveLevel(server, candidate.dimension)
                ?: return@onServerThread RandomResolution.Unsafe

            val border = level.worldBorder
            if (!intersectsRectangle(region, border.minX, border.maxX, border.minZ, border.maxZ)) {
                return@onServerThread RandomResolution.NoValidArea
            }
            if (!border.isWithinBounds(candidate.x, candidate.z)) {
                return@onServerThread RandomResolution.Unsafe
            }

            val blockX = floor(candidate.x).toInt()
            val blockZ = floor(candidate.z).toInt()
            if (!level.isLoaded(BlockPos(blockX, level.minY, blockZ))) {
                if (!settings.allowChunkGeneration) {
                    return@onServerThread RandomResolution.Unsafe
                }
                level.getChunk(blockX shr 4, blockZ shr 4)
            }

            val probe = MinecraftTeleportProbe(level, player)
            val terrainProbe = MinecraftTerrainProbe(level, probe)
            val terrainSettings = TerrainSettings(
                allowWater = settings.allowWater,
            )
            val seed = StoredPosition(
                dimension = candidate.dimension,
                x = candidate.x,
                y = level.minY.toDouble(),
                z = candidate.z,
                yaw = 0f,
                pitch = 0f,
            )
            val surface = finder.surface(seed, terrainSettings, terrainProbe)
            val requested = (surface as? TerrainResult.Found)?.position
                ?: return@onServerThread RandomResolution.Unsafe

            val safetySettings = TeleportSafetySettings(
                enabled = true,
                searchHorizontalRadius = 3,
                searchVerticalRadius = 8,
                allowWater = settings.allowWater,
            )
            when (
                val result = safety.resolve(
                    requested,
                    SafetyMode.REQUIRE_SAFE,
                    safetySettings,
                    probe,
                )
            ) {
                is SafetyResult.Safe -> RandomResolution.Safe(result.position)
                is SafetyResult.Unsafe -> RandomResolution.Unsafe
            }
        }

    private fun resolveLevel(
        server: MinecraftServer,
        dimension: String,
    ): ServerLevel? {
        val identifier = Identifier.tryParse(dimension) ?: return null
        val key = ResourceKey.create(Registries.DIMENSION, identifier)
        return server.getLevel(key)
    }

}
