package top.likoslupus.cellulosesz.tpa

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.command.messagePlayer
import top.likoslupus.cellulosesz.command.reply
import top.likoslupus.cellulosesz.command.replyError
import top.likoslupus.cellulosesz.player.PlayerResolver
import top.likoslupus.cellulosesz.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.teleport.TeleportResult
import top.likoslupus.cellulosesz.teleport.TeleportService
import top.likoslupus.cellulosesz.text.Messages

internal object TpaCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: TeleportRequestService,
        teleport: TeleportService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("tpa")
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    request(
                                        context,
                                        StringArgumentType.getString(context, "player"),
                                        service
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("tpaccept")
                    .executes { context -> accept(context, null, service, teleport, kernel) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    accept(
                                        context,
                                        StringArgumentType.getString(context, "player"),
                                        service,
                                        teleport,
                                        kernel
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("tpdeny")
                    .executes { context -> deny(context, null, service) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    deny(
                                        context,
                                        StringArgumentType.getString(context, "player"),
                                        service
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("tpcancel")
                    .executes { context -> cancel(context, service) }
        )
    }

    private fun request(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        service: TeleportRequestService,
    ): Int {
        val source = context.source
        val sender = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(Messages.prefixed("player '$targetName' is not online"))

        return when (service.send(sender.uuid, target.uuid)) {
            TpaSendResult.Self ->
                source.replyError(Messages.prefixed("you cannot request yourself"))

            TpaSendResult.Sent -> {
                target.sendSystemMessage(
                    Messages.prefixed("${sender.name.string} wants to teleport to you (use /tpaccept or /tpdeny)")
                )
                source.reply(Messages.prefixed("teleport request sent to ${target.name.string}"))
            }
        }
    }

    private fun accept(
        context: CommandContext<CommandSourceStack>,
        senderName: String?,
        service: TeleportRequestService,
        teleport: TeleportService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val accepter = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val accepterId = accepter.uuid
        val accepterName = accepter.name.string

        val senderId = senderName?.let { PlayerResolver.onlineByName(source.server, it)?.uuid }
        if (senderName != null && senderId == null) {
            return source.replyError(Messages.prefixed("player '$senderName' is not online"))
        }

        return when (val result = service.accept(accepterId, senderId)) {
            TpaAcceptResult.None ->
                source.replyError(Messages.prefixed("no pending teleport request"))

            TpaAcceptResult.Ambiguous ->
                source.replyError(Messages.prefixed("multiple pending requests; specify a player"))

            is TpaAcceptResult.Accepted -> {
                val request = result.request
                val job = kernel.launchIo {
                    val position = teleport.capturePosition(accepterId)
                    val message = if (position == null) {
                        Messages.prefixed("$accepterName is no longer online")
                    } else {
                        when (val outcome = teleport.teleport(request.senderId, position)) {
                            TeleportResult.Success -> Messages.prefixed("teleported to $accepterName")
                            TeleportResult.PlayerOffline -> Messages.prefixed("you are no longer online")
                            is TeleportResult.UnknownDimension ->
                                Messages.prefixed("teleport failed: world '${outcome.dimension}' is unavailable")
                        }
                    }
                    kernel.messagePlayer(request.senderId, message)
                    kernel.messagePlayer(accepterId, Messages.prefixed("teleport request accepted"))
                }

                if (job == null) {
                    source.replyError(Messages.prefixed("runtime is shutting down"))
                } else {
                    source.reply(Messages.prefixed("accepting teleport request..."))
                }
            }
        }
    }

    private fun deny(
        context: CommandContext<CommandSourceStack>,
        senderName: String?,
        service: TeleportRequestService,
    ): Int {
        val source = context.source
        val target = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val senderId = senderName?.let { PlayerResolver.onlineByName(source.server, it)?.uuid }
        if (senderName != null && senderId == null) {
            return source.replyError(Messages.prefixed("player '$senderName' is not online"))
        }

        return when (val result = service.deny(target.uuid, senderId)) {
            TpaDenyResult.None ->
                source.replyError(Messages.prefixed("no pending teleport request"))

            TpaDenyResult.Ambiguous ->
                source.replyError(Messages.prefixed("multiple pending requests; specify a player"))

            is TpaDenyResult.Denied -> {
                val sender = PlayerResolver.onlineById(source.server, result.request.senderId)
                sender?.sendSystemMessage(Messages.prefixed("${target.name.string} denied your teleport request"))
                source.reply(Messages.prefixed("teleport request denied"))
            }
        }
    }

    private fun cancel(
        context: CommandContext<CommandSourceStack>,
        service: TeleportRequestService,
    ): Int {
        val source = context.source
        val sender = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val request = service.cancel(sender.uuid)
            ?: return source.replyError(Messages.prefixed("you have no pending teleport request"))

        val target = PlayerResolver.onlineById(source.server, request.targetId)
        target?.sendSystemMessage(Messages.prefixed("${sender.name.string} cancelled their teleport request"))
        return source.reply(Messages.prefixed("teleport request cancelled"))
    }

}
