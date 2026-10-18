package top.likoslupus.cellulosesz.movement.request

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.movement.RequestReachabilityPort
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.*
import top.likoslupus.cellulosesz.movement.teleport.*

/** `/tpa`, `/tpahere` and `/tpaall`: creating outgoing teleport requests. */
internal object TeleportRequestSendCommands {

    fun commands(
        service: TeleportRequestService,
        tpAuto: TpAutoService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        reachability: RequestReachabilityPort,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tpa",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TPA,
            documentation = "movement/tpa",
        ) {
            argument("player", word()) { player ->
                executesPlayer {
                    send(
                        context,
                        get(player),
                        TeleportRequestType.TO_TARGET,
                        service,
                        tpAuto,
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        permissions,
                    )
                }
            }
        },

        command(
            name = "tpahere",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TPA_HERE,
            documentation = "movement/tpahere",
        ) {
            argument("player", word()) { player ->
                executesPlayer {
                    send(
                        context,
                        get(player),
                        TeleportRequestType.BRING_TARGET,
                        service,
                        tpAuto,
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        permissions,
                    )
                }
            }
        },

        command(
            name = "tpaall",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TPA_ALL,
            documentation = "movement/tpaall",
        ) {
            executesPlayer {
                tpaAll(
                    context,
                    service,
                    reachability,
                    permissions,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },
    )

    private fun send(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        type: TeleportRequestType,
        service: TeleportRequestService,
        tpAuto: TpAutoService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
    ): Int {
        val source = context.source
        val sender = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    sender.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    targetName
                ),
            )
        val bypass = source.requiresPermission(permissions, CommandPermissions.TP_TOGGLE_BYPASS)

        return when (service.send(sender.uuid, target.uuid, type, bypass)) {
            TpaSendResult.Self ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        sender.uuid,
                        MessageKey("movement.request.self")
                    )
                )

            TpaSendResult.SenderQueueFull ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        sender.uuid,
                        MessageKey("movement.request.sender_queue_full")
                    )
                )

            TpaSendResult.ServerQueueFull ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        sender.uuid,
                        MessageKey("movement.request.server_queue_full")
                    )
                )

            is TpaSendResult.TargetQueueFull ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        sender.uuid,
                        MessageKey("movement.request.target_queue_full"),
                        target.name.string,
                    ),
                )

            is TpaSendResult.TargetDisabled ->
                source.replyNotice(
                    adventure,
                    presentation,
                    sender.uuid,
                    MovementMessages.requestTargetDisabled(target.name.string),
                )

            is TpaSendResult.Sent -> {
                if (type == TeleportRequestType.TO_TARGET
                    && tpAuto.shouldAutoAccept(target.uuid, sender.uuid, type)
                ) {
                    val accepted = service.accept(target.uuid, sender.uuid)
                    if (accepted is TpaAcceptResult.Accepted) {
                        return TeleportRequestResponseCommands.launchAccepted(
                            source,
                            accepted.request,
                            auto = true,
                            teleports,
                            teleportSettings,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
                TeleportRequestCommandSupport.notify(
                    target.uuid,
                    adventure,
                    presentation,
                    MovementMessages.requestReceived(type, sender.name.string)
                )
                source.replyNotice(
                    adventure,
                    presentation,
                    sender.uuid,
                    MovementMessages.requestSent(target.name.string)
                )
            }

            is TpaSendResult.Refreshed -> {
                TeleportRequestCommandSupport.notify(
                    target.uuid,
                    adventure,
                    presentation,
                    MovementMessages.requestReceived(type, sender.name.string)
                )
                source.replyNotice(
                    adventure,
                    presentation,
                    sender.uuid,
                    MovementMessages.requestRefreshed(target.name.string),
                )
            }
        }
    }

    private fun tpaAll(
        context: CommandContext<CommandSourceStack>,
        service: TeleportRequestService,
        reachability: RequestReachabilityPort,
        permissions: PermissionService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val sender = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )

        val candidates = PlayerResolver.online(source.server).map { it.uuid }
        val result = service.sendBatch(
            senderId = sender.uuid,
            candidates = candidates,
            type = TeleportRequestType.BRING_TARGET,
            reachable = { reachability.canReach(sender.uuid, it) },
            bypassTargetChoice = source.requiresPermission(
                permissions,
                CommandPermissions.TP_TOGGLE_BYPASS,
            ),
        )

        return when (result) {
            is TpaBatchSendResult.Sent -> {
                val senderName = sender.name.string
                result.recipients.forEach { recipientId ->
                    TeleportRequestCommandSupport.notify(
                        recipientId,
                        adventure,
                        presentation,
                        MovementMessages.requestReceived(
                            TeleportRequestType.BRING_TARGET,
                            senderName
                        ),
                    )
                }
                source.replyNotice(
                    adventure,
                    presentation,
                    sender.uuid,
                    MovementMessages.requestBatchSent(result.recipients.size),
                )
            }

            TpaBatchSendResult.NoRecipients ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        sender.uuid,
                        MessageKey("movement.request.batch_none")
                    )
                )

            is TpaBatchSendResult.Cooldown ->
                source.replyNotice(
                    adventure,
                    presentation,
                    sender.uuid,
                    MovementMessages.requestBatchCooldown(result.remaining),
                )

            TpaBatchSendResult.SenderQueueFull ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        sender.uuid,
                        MessageKey("movement.request.sender_queue_full")
                    )
                )

            TpaBatchSendResult.ServerQueueFull ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        sender.uuid,
                        MessageKey("movement.request.server_queue_full")
                    )
                )
        }
    }

}
