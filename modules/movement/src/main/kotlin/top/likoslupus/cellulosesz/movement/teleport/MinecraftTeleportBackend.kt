package top.likoslupus.cellulosesz.movement.teleport

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.portal.TeleportTransition
import net.minecraft.world.phys.Vec3
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.TeleportPreflight
import top.likoslupus.cellulosesz.movement.teleport.safety.MinecraftTeleportProbe
import top.likoslupus.cellulosesz.movement.teleport.safety.SafeDestinationResolver
import top.likoslupus.cellulosesz.movement.teleport.safety.SafetyResult
import java.util.*

/**
 * The single site allowed to call the Minecraft teleport API. It performs no orchestration: delay,
 * cooldown, history, requests, and player goals all live above it in [TeleportCoordinator].
 *
 * The final compatibility veto ([TeleportPreflight]) runs here, on the server thread, after the
 * destination has been fully resolved and immediately before the teleport call, so no suspension can
 * slip in between the decision and the commit.
 *
 * Version-specific teleport code (for example a future `teleportTo(...)` path on older Minecraft)
 * must be confined to this class so Stonecutter conditions never leak into the domain.
 */
internal class MinecraftTeleportBackend(
    private val kernel: RuntimeKernel,
    private val safety: SafeDestinationResolver,
    private val preflight: TeleportPreflight = TeleportPreflight.AllowAll,
) : TeleportBackend {

    override suspend fun position(playerId: UUID): StoredPosition? =
        kernel.onServerThread {
            PlayerResolver.onlineById(kernel.requireServer(), playerId)
                    ?.toStoredPosition()
        }

    override suspend fun vanillaSpawnPosition(): StoredPosition =
        kernel.onServerThread {
            val spawn = kernel.requireServer().overworld().respawnData
            StoredPosition(
                dimension = spawn.dimension().identifier().toString(),
                x = spawn.pos().x + 0.5,
                y = spawn.pos().y.toDouble(),
                z = spawn.pos().z + 0.5,
                yaw = spawn.yaw(),
                pitch = spawn.pitch(),
            )
        }

    override suspend fun move(
        playerId: UUID,
        destination: StoredPosition,
        policy: TeleportPolicy,
        cause: TeleportCause,
    ): BackendMoveResult =
        kernel.onServerThread {
            val server = kernel.requireServer()
            val player = PlayerResolver.onlineById(server, playerId)
                ?: return@onServerThread BackendMoveResult.PlayerOffline

            val level = resolveLevel(server, destination.dimension)
                ?: return@onServerThread BackendMoveResult.UnknownDimension(destination.dimension)

            if (player.isPassenger) {
                if (!policy.dismountPassengers) {
                    return@onServerThread BackendMoveResult.PassengerConflict
                }
                player.stopRiding()
            }

            val probe = MinecraftTeleportProbe(level, player)
            val resolved = when (
                val result = safety.resolve(
                    destination,
                    policy.safetyMode,
                    policy.safety,
                    probe
                )
            ) {
                is SafetyResult.Safe ->
                    result.position

                SafetyResult.Unsafe ->
                    return@onServerThread BackendMoveResult.UnsafeDestination
            }

            if (!level.worldBorder.isWithinBounds(resolved.x, resolved.z)) {
                return@onServerThread BackendMoveResult.OutsideWorldBorder
            }

            TeleportPreflightGate.evaluate(preflight, playerId, cause, resolved)?.let { reason ->
                return@onServerThread BackendMoveResult.RejectedByHook(reason)
            }
            // Minecraft reports the outcome synchronously: a non-null return is the (same) player
            // after the move, and null means the platform refused it (a cancellable travel event) or
            // the player was removed. We record the *actual* applied position, never the request.
            val moved = player.teleport(
                TeleportTransition(
                    level,
                    Vec3(resolved.x, resolved.y, resolved.z),
                    Vec3.ZERO,
                    resolved.yaw,
                    resolved.pitch,
                ) { it.resetFallDistance() }
            ) ?: return@onServerThread BackendMoveResult.RejectedByPlatform

            BackendMoveResult.Success(moved.toStoredPosition())
        }

    private fun resolveLevel(
        server: MinecraftServer,
        dimension: String
    ): ServerLevel? {
        val identifier = Identifier.tryParse(dimension) ?: return null
        val key = ResourceKey.create(Registries.DIMENSION, identifier)
        return server.getLevel(key)
    }

}
