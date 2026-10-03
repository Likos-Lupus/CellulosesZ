package top.likoslupus.cellulosesz.communication.messaging

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.communication.CommunicationIntegration
import top.likoslupus.cellulosesz.communication.PrivateMessageObservation
import top.likoslupus.cellulosesz.communication.SendGateResult
import top.likoslupus.cellulosesz.communication.config.PrivateMessageSettings
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import java.util.*

/**
 * Owns the private-message delivery transaction. It is synchronous and server-thread confined:
 * online resolution, recipient policy and Minecraft delivery never require suspension.
 *
 * Ordering is fixed: enable check, body validation, sender gate, target resolution, reachability,
 * self check, recipient policy, delivery, reply-state commit, best-effort observation, outcome.
 */
internal class PrivateMessageService(
    private val backend: MessagingBackend,
    private val recipients: RecipientPolicy,
    private val replyState: ReplyState,
    private val settings: () -> PrivateMessageSettings,
    private val integration: () -> CommunicationIntegration,
) {

    fun send(
        senderId: UUID,
        targetName: String,
        rawBody: String,
    ): PrivateMessageOutcome {
        val config = settings()
        if (!config.enabled) {
            return PrivateMessageOutcome.Disabled
        }

        val body = MessageBody.parse(rawBody, config.maxMessageLength)
            ?: return PrivateMessageOutcome.InvalidMessage

        when (val gate = integration().senderGate.check(senderId)) {
            SendGateResult.Allowed -> Unit
            is SendGateResult.Denied -> return PrivateMessageOutcome.SenderRestricted(gate.feedback)
        }

        val sender = backend.onlineIdentityById(senderId)
            ?: return PrivateMessageOutcome.SenderOffline
        val target = backend.onlineIdentityByName(targetName)
            ?: return PrivateMessageOutcome.TargetOffline(targetName)

        if (!integration().targetReachability.canReach(senderId, target.id)) {
            return PrivateMessageOutcome.TargetOffline(targetName)
        }
        if (target.id == sender.id) {
            return PrivateMessageOutcome.SelfTarget
        }

        return deliver(sender, target, body)
    }

    fun reply(
        senderId: UUID,
        rawBody: String,
    ): PrivateMessageOutcome {
        val config = settings()
        if (!config.enabled) {
            return PrivateMessageOutcome.Disabled
        }

        val body = MessageBody.parse(rawBody, config.maxMessageLength)
            ?: return PrivateMessageOutcome.InvalidMessage

        when (val gate = integration().senderGate.check(senderId)) {
            SendGateResult.Allowed -> Unit
            is SendGateResult.Denied -> return PrivateMessageOutcome.SenderRestricted(gate.feedback)
        }

        val sender = backend.onlineIdentityById(senderId)
            ?: return PrivateMessageOutcome.SenderOffline
        val targetId = replyState.resolve(
            senderId,
            recipients.replyMode(senderId)
        )?.playerId
            ?: return PrivateMessageOutcome.NoReplyTarget
        val target = backend.onlineIdentityById(targetId)
            ?: return PrivateMessageOutcome.TargetOffline(null)

        if (!integration().targetReachability.canReach(senderId, target.id)) {
            return PrivateMessageOutcome.TargetOffline(null)
        }

        return deliver(sender, target, body)
    }

    private fun deliver(
        sender: OnlinePlayerIdentity,
        target: OnlinePlayerIdentity,
        body: MessageBody,
    ): PrivateMessageOutcome {
        when (recipients.canReceiveFrom(target.id, sender.id)) {
            ReceiveDecision.LOADING -> return PrivateMessageOutcome.RecipientStateLoading
            ReceiveDecision.DENIED -> return PrivateMessageOutcome.RecipientUnavailable
            ReceiveDecision.ALLOWED -> Unit
        }

        val incoming = CommunicationMessages.incomingPrivateMessage(sender.name, body)
        if (!backend.deliverPrivateMessage(target.id, incoming)) {
            return PrivateMessageOutcome.TargetOffline(null)
        }

        replyState.recordSuccessfulDelivery(
            senderId = sender.id,
            targetId = target.id,
            recipientMode = recipients.replyMode(target.id),
        )
        observeBestEffort(sender, target, body)

        return PrivateMessageOutcome.Delivered(target, body)
    }

    private fun observeBestEffort(
        sender: OnlinePlayerIdentity,
        target: OnlinePlayerIdentity,
        body: MessageBody,
    ) {
        try {
            integration().observer.delivered(
                PrivateMessageObservation(
                    senderId = sender.id,
                    senderName = sender.name,
                    targetId = target.id,
                    targetName = target.name,
                    text = body.value,
                )
            )
        } catch (exception: Exception) {
            LOGGER.error(
                "private-message observer failed after delivery to {}",
                target.id,
                exception,
            )
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
