package top.likoslupus.cellulosesz.application.bootstrap

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.*
import dev.architectury.platform.Platform
import kotlinx.coroutines.Dispatchers
import net.minecraft.server.level.ServerPlayer
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
            dataRoot = { dataRoot(kernel).resolve("movement") },
            settings = { config.current.movement },
        )
        val communication = createCommunicationFeature()
        val administration = createAdministrationFeature(
            kernel = kernel,
            dataRoot = { dataRoot(kernel).resolve("administration") },
            settings = { config.current.administration },
        )
        val utility = createUtilityFeature(kernel) { dataRoot(kernel) }

        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            RootCommand.register(dispatcher, config, kernel)
            movement.registerCommands(dispatcher)
            communication.registerCommands(
                dispatcher = dispatcher,
                enabled = { config.current.messaging.enabled },
                canSend = { administration.isMuted(it) },
                observe = {
                    administration.observePrivateMessage(
                        senderId = it.senderId,
                        senderName = it.senderName,
                        targetId = it.targetId,
                        targetName = it.targetName,
                        text = it.text,
                    )
                },
            )
            administration.registerCommands(dispatcher)
            utility.registerCommands(dispatcher)
        }

        LifecycleEvent.SERVER_STARTING.register {
            kernel.onServerStarting(it)
            administration.onServerStarting()
        }
        LifecycleEvent.SERVER_STARTED.register {
            kernel.onServerStarted()
            kernel.launch { config.loadOrCreate() }
        }
        LifecycleEvent.SERVER_STOPPING.register {
            movement.onServerStopping()
            administration.onServerStopping()
            kernel.onServerStopping()
        }
        LifecycleEvent.SERVER_STOPPED.register {
            kernel.onServerStopped()
        }

        TickEvent.PLAYER_POST.register { player ->
            if (player is ServerPlayer) {
                movement.onPlayerTick(player)
            }
        }
        EntityEvent.LIVING_HURT.register { entity, _, _ ->
            if (entity is ServerPlayer) {
                movement.onPlayerHurt(entity.uuid)
            }
            EventResult.pass()
        }

        PlayerEvent.PLAYER_JOIN.register {
            administration.onPlayerJoined(it)
        }

        ChatEvent.RECEIVED.register { player, _ ->
            if (player != null
                && administration.isMuted(player.uuid)
            ) {
                player.sendSystemMessage(Messages.prefixed("you are muted"))
                EventResult.interruptFalse()
            } else {
                EventResult.pass()
            }
        }

        PlayerEvent.PLAYER_QUIT.register {
            administration.onPlayerQuit(it.uuid)
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
