package top.likoslupus.cellulosesz.movement.terrain

import net.minecraft.world.phys.BlockHitResult
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.config.TerrainSettings
import top.likoslupus.cellulosesz.movement.teleport.toStoredPosition
import java.util.*

/**
 * Server-thread adapter that turns `/top`, `/bottom` and `/jump` into a [TerrainResult]. It owns the
 * only block reads; the ordering decisions live in [TerrainDestinationFinder].
 */
internal interface TerrainBackend {

    suspend fun destination(
        playerId: UUID,
        target: TerrainTarget,
        settings: TerrainSettings,
    ): TerrainResult

}

internal class MinecraftTerrainBackend(
    private val kernel: RuntimeKernel,
    private val finder: TerrainDestinationFinder,
) : TerrainBackend {

    override suspend fun destination(
        playerId: UUID,
        target: TerrainTarget,
        settings: TerrainSettings,
    ): TerrainResult =
        kernel.onServerThread {
            val server = kernel.requireServer()
            val player = PlayerResolver.onlineById(server, playerId)
                ?: return@onServerThread TerrainResult.PlayerOffline
            if (!settings.enabled) {
                return@onServerThread TerrainResult.Disabled
            }

            val level = player.level()
            val probe = MinecraftTerrainProbe.forPlayer(level, player)
            val origin = player.toStoredPosition()
            when (target) {
                TerrainTarget.TOP -> finder.surface(origin, settings, probe)
                TerrainTarget.BOTTOM -> finder.bottom(origin, settings, probe)
                TerrainTarget.JUMP -> {
                    val hit = player.pick(
                        settings.maxJumpDistance.toDouble(),
                        1.0f,
                        false,
                    )
                    when (hit) {
                        is BlockHitResult -> finder.jump(
                            origin,
                            hit.blockPos.x,
                            hit.blockPos.z,
                            settings,
                            probe,
                        )

                        else -> TerrainResult.NoTarget
                    }
                }
            }
        }

}
