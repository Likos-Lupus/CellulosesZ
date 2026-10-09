package top.likoslupus.cellulosesz.movement

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.movement.home.HomeCommands
import top.likoslupus.cellulosesz.movement.home.HomeService
import top.likoslupus.cellulosesz.movement.home.JdbcHomeRepository
import top.likoslupus.cellulosesz.movement.pending.PendingTeleportService
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation
import top.likoslupus.cellulosesz.movement.request.TeleportRequestCommands
import top.likoslupus.cellulosesz.movement.request.TeleportRequestService
import top.likoslupus.cellulosesz.movement.spawn.JdbcSpawnRepository
import top.likoslupus.cellulosesz.movement.spawn.SpawnCommands
import top.likoslupus.cellulosesz.movement.spawn.SpawnService
import top.likoslupus.cellulosesz.movement.teleport.MinecraftTeleportBackend
import top.likoslupus.cellulosesz.movement.teleport.TeleportBackend
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportCommands
import top.likoslupus.cellulosesz.movement.teleport.cooldown.TeleportCooldowns
import top.likoslupus.cellulosesz.movement.teleport.history.JdbcTeleportHistoryRepository
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import top.likoslupus.cellulosesz.movement.teleport.safety.SafeDestinationResolver
import top.likoslupus.cellulosesz.movement.teleport.toStoredPosition
import top.likoslupus.cellulosesz.movement.warp.JdbcWarpRepository
import top.likoslupus.cellulosesz.movement.warp.WarpCommands
import top.likoslupus.cellulosesz.movement.warp.WarpService
import java.util.*

/**
 * Public surface of the movement bounded context. Everything else in this module is `internal`, so
 * the application only sees command registration, movement events, disconnect handling, and config.
 */
public class MovementFeature internal constructor(
    private val teleports: TeleportCoordinator,
    private val homes: HomeService,
    private val warps: WarpService,
    private val spawn: SpawnService,
    private val requests: TeleportRequestService,
    private val pending: PendingTeleportService,
    private val backend: TeleportBackend,
    private val history: TeleportHistoryService,
    private val settings: () -> MovementSettings,
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
) {

    public fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val teleportSettings = { settings().teleport }
        HomeCommands.register(
            dispatcher,
            homes,
            backend,
            teleports,
            teleportSettings,
            kernel
        )
        WarpCommands.register(
            dispatcher,
            warps,
            backend,
            teleports,
            teleportSettings,
            kernel,
            permissions,
        )
        SpawnCommands.register(
            dispatcher,
            spawn,
            backend,
            teleports,
            teleportSettings,
            kernel,
            permissions,
        )
        TeleportRequestCommands.register(
            dispatcher,
            requests,
            teleports,
            teleportSettings,
            kernel
        )
        TeleportCommands.register(
            dispatcher,
            teleports,
            history,
            teleportSettings,
            kernel,
            permissions,
        )
    }

    /** Cancels a delayed teleport when the player moves. Must run on the server thread. */
    public fun onPlayerTick(player: ServerPlayer) {
        pending.onTick(player.uuid, player.toStoredPosition())
    }

    /** Cancels a delayed teleport when the player takes damage. Must run on the server thread. */
    public fun onPlayerHurt(playerId: UUID) {
        pending.onDamage(playerId)
    }

    /** Cancels a delayed teleport and clears request state for a leaving player. */
    public fun onPlayerQuit(playerId: UUID): List<UUID> {
        pending.cancel(playerId, TeleportCancellation.DISCONNECTED)
        return requests.clearPlayer(playerId)
    }

    /** Cancels every delayed teleport when the server stops. */
    public fun onServerStopping() {
        pending.cancelAll(TeleportCancellation.SERVER_STOPPING)
    }

}

public fun createMovementFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    permissions: PermissionService,
    settings: () -> MovementSettings,
): MovementFeature {
    val backend = MinecraftTeleportBackend(kernel, SafeDestinationResolver())
    val pending = PendingTeleportService()
    val cooldowns = TeleportCooldowns()
    val history = TeleportHistoryService(JdbcTeleportHistoryRepository(database, namespace))
    val teleports = TeleportCoordinator(
        backend = backend,
        pending = pending,
        cooldowns = cooldowns,
        history = history,
        historyEnabled = { settings().history.enabled },
    )

    return MovementFeature(
        teleports = teleports,
        homes = HomeService(JdbcHomeRepository(database, namespace)) { settings().homes },
        warps = WarpService(JdbcWarpRepository(database, namespace)),
        spawn = SpawnService(JdbcSpawnRepository(database, namespace)),
        requests = TeleportRequestService(settings = { settings().requests }),
        pending = pending,
        backend = backend,
        history = history,
        settings = settings,
        kernel = kernel,
        permissions = permissions,
    )
}
