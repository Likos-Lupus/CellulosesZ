package top.likoslupus.cellulosesz.movement.integration

import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.movement.request.*
import java.util.*

/** Teleport-request lifecycle behind [MovementIntegrationPort]. */
internal class MovementRequestIntegration(
    private val requests: TeleportRequestService,
    private val tpAuto: TpAutoService,
    private val context: MovementIntegrationContext,
) {

    suspend fun sendTeleportRequest(
        playerId: UUID,
        targetId: UUID,
    ): MovementRequestActionResult {
        when (context.verdict(playerId, CommandPermissions.TPA)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }
        return when {
            playerId == targetId -> MovementRequestActionResult.OtherFailure
            !context.online(targetId) -> MovementRequestActionResult.TargetUnavailable
            else -> when (
                context.kernel.onServerThread {
                    requests.send(
                        playerId,
                        targetId,
                        TeleportRequestType.TO_TARGET
                    )
                }
            ) {
                is TpaSendResult.Sent -> {
                    context.notifyRequestChanged(
                        playerId,
                        targetId,
                        MovementRequestState.PENDING
                    )
                    val autoAccepted = context.kernel.onServerThread {
                        tpAuto.shouldAutoAccept(
                            targetId,
                            playerId,
                            TeleportRequestType.TO_TARGET
                        ) && requests.accept(
                            targetId,
                            playerId
                        ) is TpaAcceptResult.Accepted
                    }
                    if (autoAccepted) {
                        context.notifyRequestChanged(
                            playerId,
                            targetId,
                            MovementRequestState.ACCEPTED
                        )
                        context.executeToPlayer(
                            playerId,
                            targetId
                        )
                        MovementRequestActionResult.Accepted
                    } else {
                        MovementRequestActionResult.Created(
                            requestId(
                                playerId,
                                targetId
                            )
                        )
                    }
                }

                is TpaSendResult.Refreshed ->
                    MovementRequestActionResult.AlreadyPending

                TpaSendResult.Self ->
                    MovementRequestActionResult.OtherFailure

                TpaSendResult.SenderQueueFull,
                TpaSendResult.ServerQueueFull,
                is TpaSendResult.TargetQueueFull ->
                    MovementRequestActionResult.RequestLimit

                is TpaSendResult.TargetDisabled ->
                    MovementRequestActionResult.TargetUnavailable
            }
        }
    }

    suspend fun acceptTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult {
        val parsed = parseRequestId(requestId)
            ?: return MovementRequestActionResult.NotAuthorized
        val (senderId, targetId) = parsed
        if (targetId != playerId) {
            return MovementRequestActionResult.NotAuthorized
        }
        when (context.verdict(playerId, CommandPermissions.TP_ACCEPT)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }

        return when (
            val accepted = context.kernel.onServerThread {
                requests.accept(playerId, senderId)
            }
        ) {
            is TpaAcceptResult.Accepted -> {
                context.notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.ACCEPTED
                )
                val request = accepted.request
                val subjectId = when (request.type) {
                    TeleportRequestType.TO_TARGET -> request.senderId
                    TeleportRequestType.BRING_TARGET -> request.targetId
                }
                val destinationId = when (subjectId) {
                    request.senderId -> request.targetId
                    else -> request.senderId
                }
                context.executeToPlayer(subjectId, destinationId)
                MovementRequestActionResult.Accepted
            }

            TpaAcceptResult.None -> {
                context.notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.EXPIRED
                )
                MovementRequestActionResult.Expired
            }

            TpaAcceptResult.Ambiguous ->
                MovementRequestActionResult.OtherFailure
        }
    }

    suspend fun denyTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult {
        val parsed = parseRequestId(requestId)
            ?: return MovementRequestActionResult.NotAuthorized
        val (senderId, targetId) = parsed
        if (targetId != playerId) {
            return MovementRequestActionResult.NotAuthorized
        }
        when (context.verdict(playerId, CommandPermissions.TP_DENY)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }

        return when (
            context.kernel.onServerThread {
                requests.deny(playerId, senderId)
            }
        ) {
            is TpaDenyResult.Denied -> {
                context.notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.DENIED
                )
                MovementRequestActionResult.Denied
            }

            TpaDenyResult.None -> {
                context.notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.EXPIRED
                )
                MovementRequestActionResult.Expired
            }

            TpaDenyResult.Ambiguous ->
                MovementRequestActionResult.OtherFailure
        }
    }

    suspend fun cancelOwnTeleportRequest(
        playerId: UUID,
        requestId: String,
    ): MovementRequestActionResult {
        val parsed = parseRequestId(requestId)
            ?: return MovementRequestActionResult.NotAuthorized
        val (senderId, targetId) = parsed
        if (senderId != playerId) {
            return MovementRequestActionResult.NotAuthorized
        }
        when (context.verdict(playerId, CommandPermissions.TP_CANCEL)) {
            Verdict.DENIED,
            Verdict.OFFLINE -> return MovementRequestActionResult.NotAuthorized

            Verdict.ALLOWED -> Unit
        }

        val removed = context.kernel.onServerThread {
            requests.cancelOne(playerId, targetId)
        }
        return when {
            !removed -> {
                context.notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.EXPIRED
                )
                MovementRequestActionResult.Expired
            }

            else -> {
                context.notifyRequestChanged(
                    senderId,
                    targetId,
                    MovementRequestState.CANCELLED
                )
                MovementRequestActionResult.Cancelled
            }
        }
    }

}
