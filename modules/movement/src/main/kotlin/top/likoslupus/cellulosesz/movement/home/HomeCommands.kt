package top.likoslupus.cellulosesz.movement.home

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.teleport.TeleportResult

internal object HomeCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: HomeService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("sethome")
                    .executes { context -> setHome(context, null, service, kernel) }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    setHome(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("home")
                    .executes { context -> goHome(context, null, service, kernel) }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    goHome(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("delhome")
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    deleteHome(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("homes")
                    .executes { context -> listHomes(context, service, kernel) }
        )
    }

    private fun setHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String?,
        service: HomeService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = when (service.set(playerId, rawName)) {
                SetHomeResult.Success ->
                    Messages.prefixed("home saved")

                SetHomeResult.InvalidName ->
                    Messages.prefixed("invalid home name; use a-z, 0-9, '_' or '-', up to 32 characters")

                SetHomeResult.LimitReached ->
                    Messages.prefixed("home limit reached")

                SetHomeResult.PlayerOffline ->
                    Messages.prefixed("you are no longer online")
            }
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("saving home..."))
        }
    }

    private fun goHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String?,
        service: HomeService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = when (val result = service.get(playerId, rawName)) {
                is HomeLookupResult.Found ->
                    when (
                        val teleport = service.teleportTo(playerId, result.home)
                    ) {
                        TeleportResult.Success ->
                            Messages.prefixed("teleported to home '${result.home.name.value}'")

                        TeleportResult.PlayerOffline ->
                            Messages.prefixed("you are no longer online")

                        is TeleportResult.UnknownDimension ->
                            Messages.prefixed("home dimension '${teleport.dimension}' is unavailable")
                    }

                HomeLookupResult.InvalidName ->
                    Messages.prefixed("invalid home name")

                HomeLookupResult.NotFound ->
                    Messages.prefixed("home not found")
            }
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("teleporting..."))
        }
    }

    private fun deleteHome(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        service: HomeService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = when (service.delete(playerId, rawName)) {
                DeleteHomeResult.Deleted ->
                    Messages.prefixed("home '$rawName' deleted")

                DeleteHomeResult.InvalidName ->
                    Messages.prefixed("invalid home name")

                DeleteHomeResult.NotFound ->
                    Messages.prefixed("home '$rawName' not found")
            }
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("deleting home..."))
        }
    }

    private fun listHomes(
        context: CommandContext<CommandSourceStack>,
        service: HomeService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val homes = service.list(playerId).map { it.name.value }
            val message = Messages.prefixed(
                if (homes.isEmpty()) {
                    "you have no homes"
                } else {
                    "homes: ${homes.joinToString(", ")}"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("loading homes..."))
        }
    }

}
