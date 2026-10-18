package top.likoslupus.cellulosesz.movement.request

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.preference.PreferenceWriteResult
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.movement.RequestReachabilityPort
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.teleport.*
import java.util.*

internal object TeleportRequestCommands {

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
        preferences: PlayerPreferenceService,
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

        command(
            name = "tpauto",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TPAUTO,
            documentation = "movement/tpauto",
        ) {
            executesPlayer {
                status(
                    context,
                    tpAuto,
                    preferences,
                    adventure,
                    presentation,
                )
            }
            literal("off") {
                executesPlayer {
                    setMode(
                        context,
                        TpAutoMode.OFF,
                        tpAuto,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("trusted") {
                executesPlayer {
                    setMode(
                        context,
                        TpAutoMode.TRUSTED,
                        tpAuto,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("all") {
                executesPlayer {
                    setMode(
                        context,
                        TpAutoMode.ALL,
                        tpAuto,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("list") {
                executesPlayer {
                    list(
                        context,
                        tpAuto,
                        preferences,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("allow") {
                argument("player", word()) { player ->
                    executesPlayer {
                        changeAllowed(
                            context,
                            get(player),
                            true,
                            tpAuto,
                            preferences,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
            literal("deny") {
                argument("player", word()) { player ->
                    executesPlayer {
                        changeAllowed(
                            context,
                            get(player),
                            false,
                            tpAuto,
                            preferences,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        },

        command(
            name = "tptoggle",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_TOGGLE,
            documentation = "movement/tptoggle",
        ) {
            executesPlayer {
                toggleStatus(
                    source,
                    playerId,
                    null,
                    preferences,
                    messages,
                    adventure,
                    presentation,
                )
            }
            literal("on") {
                executesPlayer {
                    applyToggle(
                        source,
                        playerId,
                        null,
                        true,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("off") {
                executesPlayer {
                    applyToggle(
                        source,
                        playerId,
                        null,
                        false,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.TP_TOGGLE_OTHERS,
            ) { player ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    toggleStatus(
                        source,
                        source.player?.uuid,
                        get(player),
                        preferences,
                        messages,
                        adventure,
                        presentation,
                    )
                }
                literal("on") {
                    executes {
                        applyToggle(
                            source,
                            source.player?.uuid,
                            get(player),
                            true,
                            preferences,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
                literal("off") {
                    executes {
                        applyToggle(
                            source,
                            source.player?.uuid,
                            get(player),
                            false,
                            preferences,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
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
                        return launchAccepted(
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
                notify(
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
                notify(
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
                    notify(
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

    private fun launchAccepted(
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
                notify(
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
            notify(
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

    private fun status(
        context: CommandContext<CommandSourceStack>,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return 0
        teleportDisabled(source, player.uuid, preferences, adventure, presentation)
                ?.let { return it }
        return source.replyNotices(
            adventure,
            presentation,
            player.uuid,
            listOf(
                MovementMessages.tpAutoStatus(tpAuto.mode(player.uuid)),
                MovementMessages.tpAutoHelp(),
            ),
        )
    }

    private fun setMode(
        context: CommandContext<CommandSourceStack>,
        mode: TpAutoMode,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return 0
        val playerId = player.uuid
        teleportDisabled(source, playerId, preferences, adventure, presentation)
                ?.let { return it }
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.updating_auto"),
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(
                    MovementMessages.tpAutoToggle(
                        tpAuto.setMode(playerId, mode)
                    )
                ),
            )
        }
    }

    private fun list(
        context: CommandContext<CommandSourceStack>,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return 0
        teleportDisabled(source, player.uuid, preferences, adventure, presentation)
                ?.let { return it }
        val names = tpAuto.allowedBy(player.uuid)
                .map {
                    PlayerResolver.onlineById(source.server, it)?.name?.string
                        ?: it.toString()
                }
        return source.replyPage(
            adventure,
            presentation,
            player.uuid,
            MovementMessages.tpAutoList(names),
        )
    }

    private fun changeAllowed(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        add: Boolean,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        teleportDisabled(source, player.uuid, preferences, adventure, presentation)
                ?.let { return it }
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    player.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    targetName
                ),
            )
        val playerId = player.uuid
        val targetId = target.uuid
        val resolvedName = target.name.string
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.updating_auto"),
        ) { feedback ->
            val notice = when {
                add ->
                    MovementMessages.tpAutoAllow(
                        resolvedName,
                        tpAuto.allow(playerId, targetId),
                    )

                else ->
                    MovementMessages.tpAutoDisallow(
                        resolvedName,
                        tpAuto.disallow(playerId, targetId),
                    )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(notice),
            )
        }
    }

    /** Rejects a `/tpauto` change while the caller has teleport requests disabled. */
    private fun teleportDisabled(
        source: CommandSourceStack,
        playerId: UUID,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int? =
        when {
            preferences.isEnabled(
                playerId,
                PlayerPreference.TELEPORT_ENABLED
            ) -> null

            else -> source.replyNotice(
                adventure,
                presentation,
                playerId,
                MovementMessages.tpAutoTeleportDisabled(),
            )
        }

    private fun toggleStatus(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        preferences: PlayerPreferenceService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val target = resolveToggleTarget(
            source,
            actorId,
            targetName,
            messages,
            adventure
        ) ?: return 0
        val enabled = preferences.isEnabled(
            target.id,
            PlayerPreference.TELEPORT_ENABLED
        )
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            MovementMessages.teleportToggleStatus(
                isOther = targetName != null,
                name = target.name,
                enabled = enabled,
            ),
        )
    }

    private fun applyToggle(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        enabled: Boolean,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val target = resolveToggleTarget(
            source,
            actorId,
            targetName,
            messages,
            adventure
        ) ?: return 0
        val targetId = target.id
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.updating_teleport"),
        ) { feedback ->
            val notice = when (
                preferences.set(
                    targetId,
                    PlayerPreference.TELEPORT_ENABLED,
                    enabled,
                )
            ) {
                PreferenceWriteResult.StorageUnavailable ->
                    MovementMessages.preferenceStorageUnavailable()

                else -> MovementMessages.teleportToggleUpdated(
                    isOther = targetName != null,
                    name = target.name,
                    enabled = enabled,
                )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                actorId,
                listOf(notice),
            )
        }
    }

    /**
     * Resolves the player a `/tptoggle` action targets. A null [targetName] means the actor
     * themselves and requires a player source; otherwise the named player must be online. Replies an
     * error and returns null when resolution fails.
     */
    private fun resolveToggleTarget(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): ToggleTarget? {
        if (targetName == null) {
            val player = source.player
                ?: run {
                    source.replyError(
                        adventure,
                        messages.renderFor(
                            null,
                            MessageKey("core.error.player_required")
                        ),
                    )
                    return null
                }
            return ToggleTarget(player.uuid, player.gameProfile.name)
        }
        val resolved = PlayerResolver.onlineByName(source.server, targetName)
            ?: run {
                source.replyError(
                    adventure,
                    messages.renderFor(
                        actorId,
                        MessageKey("movement.teleport.player_not_online"),
                        targetName,
                    ),
                )
                return null
            }
        return ToggleTarget(resolved.uuid, resolved.name.string)
    }

    private fun onlineNames(source: CommandSourceStack): List<String> =
        source.server.playerList.players.map { it.gameProfile.name }

    private data class ToggleTarget(
        val id: UUID,
        val name: String,
    )

    private fun notify(
        targetId: UUID,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        notice: Notice,
    ) {
        val audience = adventure.audience(CommandFeedbackTarget.Player(targetId))
        presentation.renderFor(targetId, notice)
                .forEach(audience::sendMessage)
    }

    private fun nameOf(source: CommandSourceStack, playerId: UUID): String =
        PlayerResolver.onlineById(
            source.server,
            playerId
        )?.name?.string ?: "player"

}
