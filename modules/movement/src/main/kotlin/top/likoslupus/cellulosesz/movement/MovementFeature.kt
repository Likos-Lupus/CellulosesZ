package top.likoslupus.cellulosesz.movement

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.movement.home.FileHomeRepository
import top.likoslupus.cellulosesz.movement.home.HomeCommands
import top.likoslupus.cellulosesz.movement.home.HomeService
import top.likoslupus.cellulosesz.movement.pending.PendingTeleportService
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation
import top.likoslupus.cellulosesz.movement.request.TeleportRequestCommands
import top.likoslupus.cellulosesz.movement.request.TeleportRequestService
import top.likoslupus.cellulosesz.movement.spawn.FileSpawnRepository
import top.likoslupus.cellulosesz.movement.spawn.SpawnCommands
import top.likoslupus.cellulosesz.movement.spawn.SpawnService
import top.likoslupus.cellulosesz.movement.teleport.MinecraftTeleportBackend
import top.likoslupus.cellulosesz.movement.teleport.TeleportBackend
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportCommands
import top.likoslupus.cellulosesz.movement.teleport.cooldown.TeleportCooldowns
import top.likoslupus.cellulosesz.movement.teleport.history.FileTeleportHistoryRepository
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import top.likoslupus.cellulosesz.movement.teleport.safety.SafeDestinationResolver
import top.likoslupus.cellulosesz.movement.teleport.toStoredPosition
import top.likoslupus.cellulosesz.movement.warp.FileWarpRepository
import top.likoslupus.cellulosesz.movement.warp.WarpCommands
import top.likoslupus.cellulosesz.movement.warp.WarpService
import java.nio.file.Path
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
            kernel
        )
        SpawnCommands.register(
            dispatcher,
            spawn,
            backend,
            teleports,
            teleportSettings,
            kernel
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
            kernel
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
    dataRoot: () -> Path,
    settings: () -> MovementSettings,
): MovementFeature {
    val backend = MinecraftTeleportBackend(kernel, SafeDestinationResolver())
    val pending = PendingTeleportService()
    val cooldowns = TeleportCooldowns()
    val history = TeleportHistoryService(FileTeleportHistoryRepository(dataRoot))
    val teleports = TeleportCoordinator(
        backend = backend,
        pending = pending,
        cooldowns = cooldowns,
        history = history,
        historyEnabled = { settings().history.enabled },
    )

    return MovementFeature(
        teleports = teleports,
        homes = HomeService(FileHomeRepository(dataRoot)) { settings().homes },
        warps = WarpService(FileWarpRepository(dataRoot)),
        spawn = SpawnService(FileSpawnRepository(dataRoot)),
        requests = TeleportRequestService(settings = { settings().requests }),
        pending = pending,
        backend = backend,
        history = history,
        settings = settings,
        kernel = kernel,
    )
}
