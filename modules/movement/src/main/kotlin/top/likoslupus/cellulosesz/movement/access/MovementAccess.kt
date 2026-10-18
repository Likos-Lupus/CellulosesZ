package top.likoslupus.cellulosesz.movement.access

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.movement.home.HomeLimits
import top.likoslupus.cellulosesz.movement.teleport.TeleportWorldAccess
import top.likoslupus.cellulosesz.movement.warp.Warp
import java.util.*

/**
 * Server-thread authorization queries shared by the movement commands and the teleport coordinator.
 * Each query re-resolves the online player from its UUID and evaluates a dynamic node, so no
 * [ServerPlayer] is ever held across suspension. A missing player means "not granted".
 */
internal class MovementAccess(
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
    private val settings: () -> MovementSettings,
) : TeleportWorldAccess {

    override suspend fun allows(subjectId: UUID, destinationDimension: String): Boolean =
        !settings().worldAccess.enabled ||
                allowsNode(
                    subjectId,
                    WorldPermissions.teleportNode(destinationDimension)
                )

    /** F17: `cellulosesz.world.home.<dimension>`, gated by `worldHomePermissions`. */
    suspend fun allowsHomeInWorld(playerId: UUID, destinationDimension: String): Boolean =
        !settings().homes.worldHomePermissions ||
                allowsNode(
                    playerId,
                    WorldPermissions.homeNode(destinationDimension)
                )

    /** F20: `cellulosesz.warp.<name>`, gated by `perWarpPermission`. */
    suspend fun allowsWarp(playerId: UUID, warpName: String): Boolean =
        !settings().warps.perWarpPermission ||
                allowsNode(
                    playerId,
                    WorldPermissions.warpNode(warpName)
                )

    /** The effective home allowance for [playerId] (F18). */
    suspend fun homeLimit(playerId: UUID): Int =
        settings().homes.let {
            return when {
                it.limits.isEmpty() -> it.maxPerPlayer

                else -> kernel.onServerThread {
                    val player = online(playerId)
                    HomeLimits.maxFor(it) { node ->
                        player != null && allows(player, node)
                    }
                }
            }
        }

    /** Filters [warps] down to the ones [playerId] may use; ungated when the toggle is off. */
    suspend fun visibleWarps(playerId: UUID, warps: List<Warp>): List<Warp> {
        if (!settings().warps.perWarpPermission) {
            return warps
        }
        return kernel.onServerThread {
            val player = online(playerId)
                ?: return@onServerThread warps
            warps.filter {
                allows(
                    player,
                    WorldPermissions.warpNode(it.name.value)
                )
            }
        }
    }

    private suspend fun allowsNode(playerId: UUID, node: String): Boolean =
        kernel.onServerThread {
            val player = online(playerId)
                ?: return@onServerThread false
            allows(player, node)
        }

    private fun allows(player: ServerPlayer, node: String): Boolean =
        permissions.allows(
            player.createCommandSourceStack(),
            WorldPermissions.spec(node, "CellulosesZ movement access"),
        )

    private fun online(playerId: UUID): ServerPlayer? =
        PlayerResolver.onlineById(kernel.requireServer(), playerId)

}
