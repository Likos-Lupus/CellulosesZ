package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.movement.request.*
import kotlin.time.Duration

internal fun MovementMessages.requestSelf(): Notice =
    notice(MessageKey("movement.request.self"))

internal fun MovementMessages.requestSenderQueueFull(): Notice =
    notice(MessageKey("movement.request.sender_queue_full"))

internal fun MovementMessages.requestServerQueueFull(): Notice =
    notice(MessageKey("movement.request.server_queue_full"))

internal fun MovementMessages.requestBatchSent(count: Int): Notice =
    success(
        MessageKey("movement.request.batch_sent"),
        count(count.toLong())
    )

internal fun MovementMessages.requestBatchNone(): Notice =
    notice(MessageKey("movement.request.batch_none"))

internal fun MovementMessages.requestBatchCooldown(remaining: Duration): Notice =
    notice(
        MessageKey("movement.request.batch_cooldown"),
        elapsed(remaining)
    )

internal fun MovementMessages.requestTargetQueueFull(target: String): Notice =
    notice(
        MessageKey("movement.request.target_queue_full"),
        playerName(target)
    )

internal fun MovementMessages.requestSent(target: String): Notice =
    success(
        MessageKey("movement.request.sent"),
        playerName(target)
    )

internal fun MovementMessages.requestRefreshed(target: String): Notice =
    success(
        MessageKey("movement.request.refreshed"),
        playerName(target)
    )

internal fun MovementMessages.requestReceived(type: TeleportRequestType, sender: String): Notice =
    Notice(
        text = LocalizedText(
            when (type) {
                TeleportRequestType.TO_TARGET ->
                    MessageKey("movement.request.received.to_target")

                TeleportRequestType.BRING_TARGET ->
                    MessageKey("movement.request.received.bring_target")
            },
            listOf(playerName(sender)),
        ),
        tone = NoticeTone.INFO,
        actions = listOf(
            UiAction.TeleportDecision(
                LocalizedText(MessageKey("movement.request.accept")),
                sender,
                accept = true
            ),
            UiAction.TeleportDecision(
                LocalizedText(MessageKey("movement.request.deny")),
                sender,
                accept = false
            ),
        ),
    )

internal fun MovementMessages.requestNone(): Notice =
    notice(MessageKey("movement.request.none"))

internal fun MovementMessages.requestAmbiguous(): Notice =
    notice(MessageKey("movement.request.ambiguous"))

internal fun MovementMessages.requestAccepted(): Notice =
    success(MessageKey("movement.request.accepted"))

internal fun MovementMessages.requestAcceptedNotify(subject: String): Notice =
    info(
        MessageKey("movement.request.accepted_notify"),
        playerName(subject)
    )

internal fun MovementMessages.requestDenied(): Notice =
    success(MessageKey("movement.request.denied"))

internal fun MovementMessages.requestDeniedNotify(target: String): Notice =
    notice(
        MessageKey("movement.request.denied_notify"),
        playerName(target)
    )

internal fun MovementMessages.requestCancelNone(): Notice =
    notice(MessageKey("movement.request.cancel_none"))

internal fun MovementMessages.requestCancelled(): Notice =
    success(MessageKey("movement.request.cancelled"))

internal fun MovementMessages.requestCancelledNotify(sender: String): Notice =
    info(
        MessageKey("movement.request.cancelled_notify"),
        playerName(sender)
    )

internal fun MovementMessages.requestTargetOffline(): Notice =
    notice(MessageKey("movement.request.target_offline"))

internal fun MovementMessages.requestTargetDisabled(target: String): Notice =
    notice(
        MessageKey("movement.request.target_disabled"),
        playerName(target),
    )

internal fun MovementMessages.teleportToggleStatus(
    isOther: Boolean,
    name: String,
    enabled: Boolean
): Notice =
    when {
        isOther && enabled -> info(
            MessageKey("movement.tptoggle.status.other.enabled"),
            playerName(name),
        )

        isOther -> info(
            MessageKey("movement.tptoggle.status.other.disabled"),
            playerName(name),
        )

        enabled -> info(MessageKey("movement.tptoggle.status.enabled"))
        else -> info(MessageKey("movement.tptoggle.status.disabled"))
    }

internal fun MovementMessages.teleportToggleUpdated(
    isOther: Boolean,
    name: String,
    enabled: Boolean
): Notice =
    when {
        isOther && enabled -> success(
            MessageKey("movement.tptoggle.other.enabled"),
            playerName(name),
        )

        isOther -> success(
            MessageKey("movement.tptoggle.other.disabled"),
            playerName(name),
        )

        enabled -> success(MessageKey("movement.tptoggle.enabled"))
        else -> success(MessageKey("movement.tptoggle.disabled"))
    }

internal fun MovementMessages.tpAutoTeleportDisabled(): Notice =
    notice(MessageKey("movement.tptoggle.tpauto_disabled"))

internal fun MovementMessages.preferenceStorageUnavailable(): Notice =
    notice(MessageKey("movement.tptoggle.storage"))

internal fun MovementMessages.tpAutoStatus(mode: TpAutoMode): Notice =
    when (mode) {
        TpAutoMode.OFF ->
            notice(MessageKey("movement.tpauto.mode.off"))

        TpAutoMode.TRUSTED ->
            success(MessageKey("movement.tpauto.mode.trusted"))

        TpAutoMode.ALL ->
            success(MessageKey("movement.tpauto.mode.all"))
    }

internal fun MovementMessages.tpAutoHelp(): Notice =
    info(MessageKey("movement.tpauto.help"))

internal fun MovementMessages.tpAutoToggle(result: TpAutoToggleResult): Notice =
    when (result) {
        is TpAutoToggleResult.Updated -> tpAutoStatus(result.mode)
        TpAutoToggleResult.StorageUnavailable ->
            notice(MessageKey("movement.tpauto.storage_unavailable"))
    }

internal fun MovementMessages.tpAutoAllow(target: String, result: TpAutoAllowResult): Notice =
    when (result) {
        TpAutoAllowResult.Added ->
            success(
                MessageKey("movement.tpauto.added"),
                playerName(target)
            )

        TpAutoAllowResult.AlreadyAllowed ->
            notice(
                MessageKey("movement.tpauto.already_allowed"),
                playerName(target)
            )

        TpAutoAllowResult.Self ->
            notice(MessageKey("movement.tpauto.self"))

        TpAutoAllowResult.LimitReached ->
            notice(MessageKey("movement.tpauto.limit_reached"))

        TpAutoAllowResult.StorageUnavailable ->
            notice(MessageKey("movement.tpauto.storage_unavailable"))
    }

internal fun MovementMessages.tpAutoDisallow(target: String, result: TpAutoDisallowResult): Notice =
    when (result) {
        TpAutoDisallowResult.Removed ->
            success(
                MessageKey("movement.tpauto.removed"),
                playerName(target)
            )

        TpAutoDisallowResult.NotAllowed ->
            notice(
                MessageKey("movement.tpauto.not_allowed"),
                playerName(target)
            )

        TpAutoDisallowResult.StorageUnavailable ->
            notice(MessageKey("movement.tpauto.storage_unavailable"))
    }

internal fun MovementMessages.tpAutoAutoAccepted(sender: String): Notice =
    info(
        MessageKey("movement.tpauto.auto_accepted"),
        playerName(sender)
    )

internal fun MovementMessages.tpAutoList(names: List<String>): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("movement.tpauto.list_title")),
        rows = names.map { name ->
            ListRow(
                main = LocalizedText(
                    MessageKey("core.value"),
                    listOf(playerName(name))
                )
            )
        },
        emptyText = LocalizedText(MessageKey("movement.tpauto.list_empty")),
    )
