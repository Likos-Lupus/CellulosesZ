package top.likoslupus.cellulosesz.application.bootstrap

import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.platform.Platform
import kotlinx.coroutines.Dispatchers
import net.minecraft.world.level.storage.LevelResource
import top.likoslupus.cellulosesz.administration.createAdministrationFeature
import top.likoslupus.cellulosesz.application.command.RootCommand
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.config.ConfigValidation
import top.likoslupus.cellulosesz.communication.createCommunicationFeature
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.KernelState
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.foundation.config.ConfigStore
import top.likoslupus.cellulosesz.movement.createMovementFeature
import top.likoslupus.cellulosesz.utility.createUtilityFeature
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The only composition root. It explicitly wires config, runtime, and the bounded-context facades;
 * it contains no domain logic.
 */
public object CellulosesZ {

    private val initialized = AtomicBoolean(false)

    public fun initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return
        }

        val kernel = RuntimeKernel()
        val config = ConfigStore(
            path = configPath(),
            serializer = CellulosesConfig.serializer(),
            defaultValue = { CellulosesConfig() },
            validate = ConfigValidation::validate,
            ioDispatcher = Dispatchers.IO,
        )

        val movement = createMovementFeature(
            kernel = kernel,
            dataRoot = { dataRoot(kernel) },
            homeSettings = { config.current.homes },
            requestSettings = { config.current.teleportRequests },
        )
        val communication = createCommunicationFeature()
        val administration = createAdministrationFeature()
        val utility = createUtilityFeature(kernel) { dataRoot(kernel) }

        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            RootCommand.register(dispatcher, config, kernel)
            movement.registerCommands(dispatcher)
            communication.registerCommands(dispatcher) { config.current.messaging.enabled }
            administration.registerCommands(dispatcher)
            utility.registerCommands(dispatcher)
        }

        LifecycleEvent.SERVER_STARTING.register {
            kernel.onServerStarting(it)
        }
        LifecycleEvent.SERVER_STARTED.register {
            kernel.onServerStarted()
            kernel.launchIo { config.loadOrCreate() }
        }
        LifecycleEvent.SERVER_STOPPING.register {
            kernel.onServerStopping()
        }
        LifecycleEvent.SERVER_STOPPED.register {
            kernel.onServerStopped()
        }

        PlayerEvent.PLAYER_QUIT.register {
            val affected = movement.onPlayerQuit(it.uuid)
            communication.onPlayerQuit(it.uuid)
            if (kernel.state == KernelState.RUNNING) {
                affected.forEach { senderId ->
                    PlayerResolver.onlineById(
                        kernel.requireServer(),
                        senderId
                    )?.sendSystemMessage(
                        Messages.prefixed("teleport request cancelled: target went offline")
                    )
                }
            }
        }
    }

    private fun configPath(): Path =
        Platform.getConfigFolder()
                .resolve("cellulosesz")
                .resolve("cellulosesz.jsonc")

    private fun dataRoot(kernel: RuntimeKernel): Path =
        kernel.requireServer().getWorldPath(LevelResource.ROOT)
                .resolve("cellulosesz")

}
