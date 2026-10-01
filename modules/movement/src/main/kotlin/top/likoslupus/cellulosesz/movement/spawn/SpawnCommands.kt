package top.likoslupus.cellulosesz.movement.spawn

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permissions
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.teleport.TeleportResult

internal object SpawnCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: SpawnService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("spawn")
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(Messages.prefixed("this command requires a player"))
                        val job = kernel.launchIo {
                            val message = when (
                                val result = service.teleportToSpawn(playerId)
                            ) {
                                TeleportResult.Success ->
                                    Messages.prefixed("teleported to spawn")

                                TeleportResult.PlayerOffline ->
                                    Messages.prefixed("you are no longer online")

                                is TeleportResult.UnknownDimension ->
                                    Messages.prefixed("spawn dimension '${result.dimension}' is unavailable")
                            }
                            kernel.messagePlayer(playerId, message)
                        }

                        if (job == null) {
                            source.replyError(Messages.prefixed("runtime is shutting down"))
                        } else {
                            source.reply(Messages.prefixed("teleporting..."))
                        }
                    }
        )
        dispatcher.register(
            Commands.literal("setspawn")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(Messages.prefixed("this command requires a player"))
                        val job = kernel.launchIo {
                            val message = when (service.set(playerId)) {
                                SetSpawnResult.Success ->
                                    Messages.prefixed("spawn set")

                                SetSpawnResult.PlayerOffline ->
                                    Messages.prefixed("you are no longer online")
                            }
                            kernel.messagePlayer(playerId, message)
                        }

                        if (job == null) {
                            source.replyError(Messages.prefixed("runtime is shutting down"))
                        } else {
                            source.reply(Messages.prefixed("saving spawn..."))
                        }
                    }
        )
        dispatcher.register(
            Commands.literal("delspawn")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(Messages.prefixed("this command requires a player"))
                        val job = kernel.launchIo {
                            val message = when (service.clear()) {
                                ClearSpawnResult.Cleared ->
                                    Messages.prefixed("spawn reset to vanilla")

                                ClearSpawnResult.NotConfigured ->
                                    Messages.prefixed("no custom spawn configured")
                            }
                            kernel.messagePlayer(playerId, message)
                        }

                        if (job == null) {
                            source.replyError(Messages.prefixed("runtime is shutting down"))
                        } else {
                            source.reply(Messages.prefixed("clearing spawn..."))
                        }
                    }
        )
    }

}
