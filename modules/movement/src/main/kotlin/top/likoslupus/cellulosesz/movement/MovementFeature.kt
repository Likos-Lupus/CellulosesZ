package top.likoslupus.cellulosesz.movement

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.movement.config.HomeSettings
import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings
import top.likoslupus.cellulosesz.movement.home.FileHomeRepository
import top.likoslupus.cellulosesz.movement.home.HomeCommands
import top.likoslupus.cellulosesz.movement.home.HomeService
import top.likoslupus.cellulosesz.movement.request.TeleportRequestService
import top.likoslupus.cellulosesz.movement.request.TpaCommands
import top.likoslupus.cellulosesz.movement.spawn.FileSpawnRepository
import top.likoslupus.cellulosesz.movement.spawn.SpawnCommands
import top.likoslupus.cellulosesz.movement.spawn.SpawnService
import top.likoslupus.cellulosesz.movement.teleport.TeleportService
import top.likoslupus.cellulosesz.movement.warp.FileWarpRepository
import top.likoslupus.cellulosesz.movement.warp.WarpCommands
import top.likoslupus.cellulosesz.movement.warp.WarpService
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * Public surface of the movement bounded context. Everything else in this module is `internal`,
 * so the application only sees command registration, disconnect handling, and config types.
 */
public class MovementFeature internal constructor(
    private val homeService: HomeService,
    private val warpService: WarpService,
    private val spawnService: SpawnService,
    private val requestService: TeleportRequestService,
    private val teleportService: TeleportService,
    private val kernel: RuntimeKernel,
) {

    public fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        HomeCommands.register(dispatcher, homeService, kernel)
        WarpCommands.register(dispatcher, warpService, kernel)
        SpawnCommands.register(dispatcher, spawnService, kernel)
        TpaCommands.register(dispatcher, requestService, teleportService, kernel)
    }

    /** Clears transient request state for a leaving player; returns senders to notify. */
    public fun onPlayerQuit(playerId: UUID): List<UUID> =
        requestService.clearPlayer(playerId)

}

public fun createMovementFeature(
    kernel: RuntimeKernel,
    dataRoot: () -> Path,
    homeSettings: () -> HomeSettings,
    requestSettings: () -> TeleportRequestSettings,
): MovementFeature {
    val teleport = TeleportService(kernel)
    return MovementFeature(
        homeService = HomeService(
            FileHomeRepository(dataRoot),
            teleport,
            homeSettings
        ),
        warpService = WarpService(
            FileWarpRepository(dataRoot),
            teleport
        ),
        spawnService = SpawnService(
            FileSpawnRepository(dataRoot),
            teleport
        ),
        requestService = TeleportRequestService(
            timeout = { requestSettings().timeoutSeconds.seconds },
        ),
        teleportService = teleport,
        kernel = kernel,
    )
}
