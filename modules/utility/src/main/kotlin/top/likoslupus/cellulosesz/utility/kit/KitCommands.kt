package top.likoslupus.cellulosesz.utility.kit

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.canUseModeratorCommands
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

internal object KitCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("kit")
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    give(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("kits")
                    .executes { context -> list(context, service, kernel) }
        )
        dispatcher.register(
            Commands.literal("createkit")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    create(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("delkit")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    delete(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel
                                    )
                                }
                    )
        )
    }

    private fun give(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = Messages.prefixed(
                when (service.give(playerId, name)) {
                    GiveKitResult.Success -> "received kit '$name'"
                    GiveKitResult.NotFound -> "kit '$name' not found"
                    GiveKitResult.PlayerOffline -> "you are no longer online"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("giving kit..."))
        }
    }

    private fun list(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val kits = service.list().map { it.value }
            val message = Messages.prefixed(
                when {
                    kits.isEmpty() -> "no kits defined"
                    else -> "kits: ${kits.joinToString(", ")}"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("loading kits..."))
        }
    }

    private fun create(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launchIo {
            val message = Messages.prefixed(
                when (service.create(playerId, name)) {
                    CreateKitResult.Success -> "kit '$name' created from your inventory"
                    CreateKitResult.InvalidName -> "invalid kit name"
                    CreateKitResult.Empty -> "your inventory is empty"
                    CreateKitResult.PlayerOffline -> "you are no longer online"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("creating kit..."))
        }
    }

    private fun delete(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val job = kernel.launchIo {
            val message = Messages.prefixed(
                when (service.delete(name)) {
                    DeleteKitResult.Deleted -> "kit '$name' deleted"
                    DeleteKitResult.InvalidName -> "invalid kit name"
                    DeleteKitResult.NotFound -> "kit '$name' not found"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return if (job == null) {
            source.replyError(Messages.prefixed("runtime is shutting down"))
        } else {
            source.reply(Messages.prefixed("deleting kit..."))
        }
    }

}
