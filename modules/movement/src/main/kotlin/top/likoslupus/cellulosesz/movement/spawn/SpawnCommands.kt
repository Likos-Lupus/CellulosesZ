package top.likoslupus.cellulosesz.movement.spawn

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.canUseModeratorCommands
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.command.launchResult
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportFeedback

internal object SpawnCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: SpawnService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("spawn")
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(
                                Messages.prefixed("this command requires a player")
                            )

                        val job = kernel.launch {
                            val position = service.configured() ?: backend.vanillaSpawnPosition()
                            val intent = TeleportIntent(
                                subjectId = playerId,
                                destination = TeleportDestination.Fixed(position),
                                cause = TeleportCause.SPAWN,
                                policy = teleportPolicyFor(TeleportCause.SPAWN, teleportSettings()),
                            )
                            val message = when (val outcome = teleports.execute(intent)) {
                                is TeleportOutcome.Success -> Messages.prefixed("teleported to spawn")
                                else -> TeleportFeedback.failure(outcome) ?: return@launch
                            }
                            kernel.messagePlayer(playerId, message)
                        }

                        launchResult(source, job, "teleporting...")
                    }
        )
        dispatcher.register(
            Commands.literal("setspawn")
                    .requires { it.canUseModeratorCommands() }
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(
                                Messages.prefixed("this command requires a player")
                            )

                        val job = kernel.launch {
                            val position = backend.position(playerId)
                            val message = if (position == null) {
                                Messages.prefixed("you are no longer online")
                            } else {
                                service.set(position)
                                Messages.prefixed("spawn set")
                            }
                            kernel.messagePlayer(playerId, message)
                        }

                        launchResult(source, job, "saving spawn...")
                    }
        )
        dispatcher.register(
            Commands.literal("delspawn")
                    .requires { it.canUseModeratorCommands() }
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(
                                Messages.prefixed("this command requires a player")
                            )

                        val job = kernel.launch {
                            val message = Messages.prefixed(
                                when (service.clear()) {
                                    ClearSpawnResult.Cleared -> "spawn reset to vanilla"
                                    ClearSpawnResult.NotConfigured -> "no custom spawn configured"
                                }
                            )
                            kernel.messagePlayer(playerId, message)
                        }

                        launchResult(source, job, "clearing spawn...")
                    }
        )
    }

}
