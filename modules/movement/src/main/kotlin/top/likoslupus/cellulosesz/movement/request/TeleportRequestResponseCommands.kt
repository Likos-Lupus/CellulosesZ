package top.likoslupus.cellulosesz.movement.request

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.*
import top.likoslupus.cellulosesz.movement.teleport.*
import java.util.*

/** `/tpaccept`, `/tpdeny` and `/tpcancel`: answering requests already addressed to the caller. */
internal object TeleportRequestResponseCommands {

    fun commands(
        service: TeleportRequestService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tpaccept",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_ACCEPT,
            documentation = "movement/tpaccept",
        ) {
            executesPlayer {
                accept(
                    context,
                    null,
                    service,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("player", word()) { player ->
                executesPlayer {
                    accept(
                        context,
                        get(player),
                        service,
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "tpdeny",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_DENY,
            documentation = "movement/tpdeny",
        ) {
            executesPlayer {
                deny(
                    context,
                    null,
                    service,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("player", word()) { player ->
                executesPlayer {
                    deny(
                        context,
                        get(player),
                        service,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "tpcancel",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_CANCEL,
            documentation = "movement/tpcancel",
        ) {
            executesPlayer {
                cancel(
                    context,
                    service,
                    messages,
                    adventure,
                    presentation,
                )
            }
        },
    )

    private fun accept(
        context: CommandContext<CommandSourceStack>,
        senderName: String?,
        service: TeleportRequestService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val accepter = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )

        val senderId = senderName?.let {
            PlayerResolver.onlineByName(source.server, it)?.uuid
        }
        if (senderName != null && senderId == null) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    accepter.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    senderName
                ),
            )
        }

        return when (val result = service.accept(accepter.uuid, senderId)) {
            TpaAcceptResult.None ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        accepter.uuid,
                        MessageKey("movement.request.none")
                    )
                )

            TpaAcceptResult.Ambiguous ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        accepter.uuid,
                        MessageKey("movement.request.ambiguous")
                    )
                )

            is TpaAcceptResult.Accepted ->
                launchAccepted(
                    source,
                    result.request,
                    auto = false,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
        }
    }

    /** Launches an accepted request; also used by `/tpa` when the target auto-accepts. */
    internal fun launchAccepted(
        source: CommandSourceStack,
        request: TeleportRequest,
        auto: Boolean,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val subjectId = when (request.type) {
            TeleportRequestType.TO_TARGET -> request.senderId
            TeleportRequestType.BRING_TARGET -> request.targetId
        }
        val destinationId = when (subjectId) {
            request.senderId -> request.targetId
            else -> request.senderId
        }
        val subjectName = nameOf(source, subjectId)

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.accept_request"),
        ) { _ ->
            val intent = TeleportIntent(
                subjectId = subjectId,
                destination = TeleportDestination.Player(destinationId),
                cause = TeleportCause.REQUEST,
                policy = teleportPolicyFor(
                    TeleportCause.REQUEST,
                    teleportSettings()
                ),
            )
            val notices = when (val outcome = teleports.execute(intent)) {
                is TeleportOutcome.Success -> listOf(MovementMessages.requestAccepted())
                else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
            }
            kernel.sendNotices(
                CommandFeedbackTarget.Player(subjectId),
                adventure,
                presentation,
                subjectId,
                notices,
            )
            val accepterNotice = when {
                auto -> MovementMessages.tpAutoAutoAccepted(subjectName)
                else -> MovementMessages.requestAcceptedNotify(subjectName)
            }
            kernel.sendNotices(
                CommandFeedbackTarget.Player(destinationId),
                adventure,
                presentation,
                destinationId,
                listOf(accepterNotice),
            )
        }
    }

    private fun deny(
        context: CommandContext<CommandSourceStack>,
        senderName: String?,
        service: TeleportRequestService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val target = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val senderId = senderName?.let {
            PlayerResolver.onlineByName(source.server, it)?.uuid
        }
        if (senderName != null && senderId == null) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    target.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    senderName
                ),
            )
        }

        return when (val result = service.deny(target.uuid, senderId)) {
            TpaDenyResult.None ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        target.uuid,
                        MessageKey("movement.request.none")
                    )
                )

            TpaDenyResult.Ambiguous ->
                source.replyError(
                    adventure,
                    messages.renderFor(
                        target.uuid,
                        MessageKey("movement.request.ambiguous")
                    )
                )

            is TpaDenyResult.Denied -> {
                TeleportRequestCommandSupport.notify(
                    result.request.senderId,
                    adventure,
                    presentation,
                    MovementMessages.requestDeniedNotify(target.name.string),
                )
                source.replyNotice(
                    adventure,
                    presentation,
                    target.uuid,
                    MovementMessages.requestDenied()
                )
            }
        }
    }

    private fun cancel(
        context: CommandContext<CommandSourceStack>,
        service: TeleportRequestService,
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

        val cancelled = service.cancel(sender.uuid)
        if (cancelled.isEmpty()) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    sender.uuid,
                    MessageKey("movement.request.cancel_none")
                )
            )
        }

        cancelled.forEach { request ->
            TeleportRequestCommandSupport.notify(
                request.targetId,
                adventure,
                presentation,
                MovementMessages.requestCancelledNotify(sender.name.string),
            )
        }
        return source.replyNotice(
            adventure,
            presentation,
            sender.uuid,
            MovementMessages.requestCancelled()
        )
    }

    private fun nameOf(source: CommandSourceStack, playerId: UUID): String =
        PlayerResolver.onlineById(
            source.server,
            playerId
        )?.name?.string ?: "player"

}
