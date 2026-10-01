package top.likoslupus.cellulosesz.bootstrap

import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.platform.Platform
import net.minecraft.world.level.storage.LevelResource
import top.likoslupus.cellulosesz.command.RootCommand
import top.likoslupus.cellulosesz.config.ConfigService
import top.likoslupus.cellulosesz.home.FileHomeRepository
import top.likoslupus.cellulosesz.home.HomeCommands
import top.likoslupus.cellulosesz.home.HomeService
import top.likoslupus.cellulosesz.item.ItemCommands
import top.likoslupus.cellulosesz.kit.FileKitRepository
import top.likoslupus.cellulosesz.kit.KitCommands
import top.likoslupus.cellulosesz.kit.KitService
import top.likoslupus.cellulosesz.messaging.ConversationState
import top.likoslupus.cellulosesz.messaging.MessagingCommands
import top.likoslupus.cellulosesz.moderation.ModerationCommands
import top.likoslupus.cellulosesz.player.PlayerResolver
import top.likoslupus.cellulosesz.playerstate.PlayerStateCommands
import top.likoslupus.cellulosesz.runtime.KernelState
import top.likoslupus.cellulosesz.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.spawn.FileSpawnRepository
import top.likoslupus.cellulosesz.spawn.SpawnCommands
import top.likoslupus.cellulosesz.spawn.SpawnService
import top.likoslupus.cellulosesz.teleport.TeleportService
import top.likoslupus.cellulosesz.text.Messages
import top.likoslupus.cellulosesz.tpa.TeleportRequestService
import top.likoslupus.cellulosesz.tpa.TpaCommands
import top.likoslupus.cellulosesz.warp.FileWarpRepository
import top.likoslupus.cellulosesz.warp.WarpCommands
import top.likoslupus.cellulosesz.warp.WarpService
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

object CellulosesZ {

    private val initialized = AtomicBoolean(false)

    fun configPath(): Path =
        Platform.getConfigFolder().resolve("cellulosesz").resolve("cellulosesz.jsonc")

    private fun dataRoot(kernel: RuntimeKernel): Path =
        kernel.requireServer().getWorldPath(LevelResource.ROOT).resolve("cellulosesz")

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return
        }

        val kernel = RuntimeKernel()
        val config = ConfigService(configPath())

        val teleport = TeleportService(kernel)
        val homeService = HomeService(
            FileHomeRepository { dataRoot(kernel) },
            teleport
        ) { config.current.homes }
        val requests = TeleportRequestService(
            timeout = { Duration.ofSeconds(config.current.teleportRequests.timeoutSeconds) },
        )
        val conversation = ConversationState()

        val warpService = WarpService(FileWarpRepository { dataRoot(kernel) }, teleport)
        val spawnService = SpawnService(FileSpawnRepository { dataRoot(kernel) }, teleport)
        val kitService = KitService(FileKitRepository { dataRoot(kernel) }, kernel)

        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            RootCommand.register(dispatcher, config, kernel)
            HomeCommands.register(dispatcher, homeService, kernel)
            TpaCommands.register(dispatcher, requests, teleport, kernel)
            MessagingCommands.register(
                dispatcher,
                conversation
            ) { config.current.messaging.enabled }
            WarpCommands.register(dispatcher, warpService, kernel)
            SpawnCommands.register(dispatcher, spawnService, kernel)
            KitCommands.register(dispatcher, kitService, kernel)
            ModerationCommands.register(dispatcher)
            PlayerStateCommands.register(dispatcher)
            ItemCommands.register(dispatcher)
        }

        LifecycleEvent.SERVER_STARTING.register { server -> kernel.onServerStarting(server) }
        LifecycleEvent.SERVER_STARTED.register {
            kernel.onServerStarted()
            kernel.launchIo { config.loadOrCreate() }
        }
        LifecycleEvent.SERVER_STOPPING.register { kernel.onServerStopping() }
        LifecycleEvent.SERVER_STOPPED.register { kernel.onServerStopped() }

        PlayerEvent.PLAYER_QUIT.register { player ->
            val affected = requests.clearPlayer(player.uuid)
            conversation.clear(player.uuid)
            if (kernel.state == KernelState.RUNNING) {
                affected.forEach { senderId ->
                    PlayerResolver.onlineById(kernel.requireServer(), senderId)?.sendSystemMessage(
                        Messages.prefixed("teleport request cancelled: target went offline")
                    )
                }
            }
        }
    }

}
