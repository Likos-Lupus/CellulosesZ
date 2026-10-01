package top.likoslupus.cellulosesz.movement.request

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.command.launchResult
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportFeedback
import java.util.*

internal object TeleportRequestCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: TeleportRequestService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("tpa")
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    send(
                                        context,
                                        StringArgumentType.getString(context, "player"),
                                        TeleportRequestType.TO_TARGET,
                                        service,
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("tpahere")
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    send(
                                        context,
                                        StringArgumentType.getString(context, "player"),
                                        TeleportRequestType.BRING_TARGET,
                                        service,
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("tpaccept")
                    .executes { context ->
                        accept(
                            context,
                            null,
                            service,
                            teleports,
                            teleportSettings,
                            kernel
                        )
                    }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .executes { context ->
                                    accept(
                                        context,
                                        StringArgumentType.getString(context, "player"),
                                        service,
                                        teleports,
                                        teleportSettings,
                                        kernel,
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

    private fun send(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        type: TeleportRequestType,
        service: TeleportRequestService,
    ): Int {
        val source = context.source
        val sender = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(Messages.prefixed("player '$targetName' is not online"))

        val verb = if (type == TeleportRequestType.TO_TARGET) {
            "wants to teleport to you"
        } else {
            "wants you to teleport to them"
        }

        return when (service.send(sender.uuid, target.uuid, type)) {
            TpaSendResult.Self ->
                source.replyError(Messages.prefixed("you cannot request yourself"))

            TpaSendResult.SenderAlreadyHasRequest ->
                source.replyError(Messages.prefixed("you already have a pending request; use /tpcancel"))

            TpaSendResult.TargetQueueFull ->
                source.replyError(Messages.prefixed("${target.name.string} has too many pending requests"))

            is TpaSendResult.Sent -> {
                target.sendSystemMessage(
                    Messages.prefixed("${sender.name.string} $verb (use /tpaccept or /tpdeny)")
                )
                source.reply(Messages.prefixed("teleport request sent to ${target.name.string}"))
            }

            is TpaSendResult.Refreshed -> {
                target.sendSystemMessage(
                    Messages.prefixed("${sender.name.string} $verb (use /tpaccept or /tpdeny)")
                )
                source.reply(Messages.prefixed("teleport request to ${target.name.string} refreshed"))
            }
        }
    }

    private fun accept(
        context: CommandContext<CommandSourceStack>,
        senderName: String?,
        service: TeleportRequestService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val accepter = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val senderId = senderName?.let { PlayerResolver.onlineByName(source.server, it)?.uuid }
        if (senderName != null && senderId == null) {
            return source.replyError(Messages.prefixed("player '$senderName' is not online"))
        }

        return when (val result = service.accept(accepter.uuid, senderId)) {
            TpaAcceptResult.None ->
                source.replyError(Messages.prefixed("no pending teleport request"))

            TpaAcceptResult.Ambiguous ->
                source.replyError(Messages.prefixed("multiple pending requests; specify a player"))

            is TpaAcceptResult.Accepted -> {
                val request = result.request
                val subjectId = when (request.type) {
                    TeleportRequestType.TO_TARGET -> request.senderId
                    TeleportRequestType.BRING_TARGET -> request.targetId
                }
                val destinationId = if (subjectId == request.senderId) request.targetId else request.senderId
                val subjectName = nameOf(source, subjectId)

                val job = kernel.launch {
                    val intent = TeleportIntent(
                        subjectId = subjectId,
                        destination = TeleportDestination.Player(destinationId),
                        cause = TeleportCause.REQUEST,
                        policy = teleportPolicyFor(TeleportCause.REQUEST, teleportSettings()),
                    )
                    val message = when (val outcome = teleports.execute(intent)) {
                        is TeleportOutcome.Success ->
                            Messages.prefixed("teleport request accepted")

                        else -> TeleportFeedback.failure(outcome) ?: return@launch
                    }
                    kernel.messagePlayer(subjectId, message)
                    kernel.messagePlayer(
                        destinationId,
                        Messages.prefixed("$subjectName accepted the teleport request"),
                    )
                }

                launchResult(source, job, "accepting teleport request...")
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
                PlayerResolver.onlineById(source.server, result.request.senderId)
                        ?.sendSystemMessage(
                            Messages.prefixed("${target.name.string} denied your teleport request")
                        )
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

        PlayerResolver.onlineById(source.server, request.targetId)
                ?.sendSystemMessage(
                    Messages.prefixed("${sender.name.string} cancelled their teleport request")
                )
        return source.reply(Messages.prefixed("teleport request cancelled"))
    }

    private fun nameOf(source: CommandSourceStack, playerId: UUID): String =
        PlayerResolver.onlineById(source.server, playerId)?.name?.string ?: "player"

}
