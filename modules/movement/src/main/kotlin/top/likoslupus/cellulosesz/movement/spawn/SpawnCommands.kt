package top.likoslupus.cellulosesz.movement.spawn

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.command.launchResult
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportFeedback

internal object SpawnCommands {

    fun commands(
        service: SpawnService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "spawn",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SPAWN,
            documentation = "movement/spawn",
        ) {
            executesPlayer {
                teleportToSpawn(
                    context,
                    service,
                    backend,
                    teleports,
                    teleportSettings,
                    kernel
                )
            }
        },
        command(
            name = "setspawn",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SET_SPAWN,
            documentation = "movement/setspawn",
        ) {
            executesPlayer {
                setSpawn(
                    context,
                    service,
                    backend,
                    kernel
                )
            }
        },
        command(
            name = "delspawn",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.DEL_SPAWN,
            documentation = "movement/delspawn",
        ) {
            executesPlayer {
                deleteSpawn(
                    context,
                    service,
                    kernel
                )
            }
        },
    )

    private fun teleportToSpawn(
        context: CommandContext<CommandSourceStack>,
        service: SpawnService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val position = service.configured() ?: backend.vanillaSpawnPosition()
            val intent = TeleportIntent(
                subjectId = playerId,
                destination = TeleportDestination.Fixed(position),
                cause = TeleportCause.SPAWN,
                policy = teleportPolicyFor(
                    TeleportCause.SPAWN,
                    teleportSettings()
                ),
            )
            val message = when (val outcome = teleports.execute(intent)) {
                is TeleportOutcome.Success -> Messages.prefixed("teleported to spawn")
                else -> TeleportFeedback.failure(outcome)
                    ?: return@launch
            }
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(
            source,
            job,
            "teleporting..."
        )
    }

    private fun setSpawn(
        context: CommandContext<CommandSourceStack>,
        service: SpawnService,
        backend: TeleportBackend,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val message = when (val position = backend.position(playerId)) {
                null -> Messages.prefixed("you are no longer online")
                else -> {
                    service.set(position)
                    Messages.prefixed("spawn set")
                }
            }
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(
            source,
            job,
            "saving spawn..."
        )
    }

    private fun deleteSpawn(
        context: CommandContext<CommandSourceStack>,
        service: SpawnService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val message = Messages.prefixed(
                when (service.clear()) {
                    ClearSpawnResult.Cleared -> "spawn reset to vanilla"
                    ClearSpawnResult.NotConfigured -> "no custom spawn configured"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(
            source,
            job,
            "clearing spawn..."
        )
    }

}
