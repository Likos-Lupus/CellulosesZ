package top.likoslupus.cellulosesz.warp

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permissions
import top.likoslupus.cellulosesz.command.messagePlayer
import top.likoslupus.cellulosesz.command.reply
import top.likoslupus.cellulosesz.command.replyError
import top.likoslupus.cellulosesz.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.teleport.TeleportResult
import top.likoslupus.cellulosesz.text.Messages

internal object WarpCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: WarpService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("warp")
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    useWarp(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("warps")
                    .executes { context -> listWarps(context, service, kernel) }
        )
        dispatcher.register(
            Commands.literal("setwarp")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    setWarp(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("delwarp")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    deleteWarp(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
    }

    private fun useWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = when (val result = service.get(name)) {
                is WarpLookupResult.Found ->
                    when (val teleport = service.teleportTo(
                        playerId,
                        result.warp
                    )) {
                        TeleportResult.Success ->
                            Messages.prefixed("teleported to warp '$name'")

                        TeleportResult.PlayerOffline ->
                            Messages.prefixed("you are no longer online")

                        is TeleportResult.UnknownDimension ->
                            Messages.prefixed("warp dimension '${teleport.dimension}' is unavailable")
                    }

                WarpLookupResult.InvalidName ->
                    Messages.prefixed("invalid warp name")

                WarpLookupResult.NotFound ->
                    Messages.prefixed("warp '$name' not found")
            }
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("teleporting..."))
        }
    }

    private fun listWarps(
        context: CommandContext<CommandSourceStack>,
        service: WarpService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val warps = service.list().map { it.name.value }
            val message = if (warps.isEmpty()) {
                Messages.prefixed("no warps defined")
            } else {
                Messages.prefixed("warps: ${warps.joinToString(", ")}")
            }
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("loading warps..."))
        }
    }

    private fun setWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = when (service.set(playerId, name)) {
                SetWarpResult.Success ->
                    Messages.prefixed("warp '$name' created")

                SetWarpResult.InvalidName ->
                    Messages.prefixed("invalid warp name")

                SetWarpResult.PlayerOffline ->
                    Messages.prefixed("you are no longer online")
            }
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("saving warp..."))
        }
    }

    private fun deleteWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = when (service.delete(name)) {
                DeleteWarpResult.Deleted ->
                    Messages.prefixed("warp '$name' deleted")

                DeleteWarpResult.InvalidName ->
                    Messages.prefixed("invalid warp name")

                DeleteWarpResult.NotFound ->
                    Messages.prefixed("warp '$name' not found")
            }
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("deleting warp..."))
        }
    }

}
