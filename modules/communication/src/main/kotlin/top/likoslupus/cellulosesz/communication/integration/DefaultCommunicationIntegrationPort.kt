package top.likoslupus.cellulosesz.communication.integration

import top.likoslupus.cellulosesz.communication.announcement.AnnouncementResult
import top.likoslupus.cellulosesz.communication.announcement.AnnouncementService
import top.likoslupus.cellulosesz.communication.mail.MailSendResult
import top.likoslupus.cellulosesz.communication.mail.MailSender
import top.likoslupus.cellulosesz.communication.mail.MailService
import top.likoslupus.cellulosesz.communication.mail.MailSummaryResult
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageOutcome
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageService
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferencesService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import java.time.Duration
import java.util.*

/**
 * The production [CommunicationIntegrationPort]. It owns no policy: every call is routed into the
 * existing private-message, mail and announcement services. Identifier resolution and the server
 * thread hop are the only orchestration here.
 */
internal class DefaultCommunicationIntegrationPort(
    private val runner: ServerThreadRunner,
    private val known: KnownPlayerResolver,
    private val privateMessages: PrivateMessageService,
    private val preferences: MessagingPreferencesService,
    private val mail: MailService,
    private val announcements: AnnouncementService,
) : CommunicationIntegrationPort {

    override suspend fun sendPrivateMessage(
        senderId: UUID,
        recipientId: UUID,
        body: String,
    ): PrivateMessagePortOutcome =
        runner.run {
            val targetName = known.onlineById(recipientId)?.name
                ?: known.knownById(recipientId)?.name
                ?: return@run PrivateMessagePortOutcome.RecipientUnavailable
            privateMessages.send(senderId, targetName, body).toPortOutcome()
        }

    override suspend fun replyPrivateMessage(
        senderId: UUID,
        body: String,
    ): PrivateMessagePortOutcome =
        runner.run { privateMessages.reply(senderId, body).toPortOutcome() }

    override fun receivingEnabled(playerId: UUID): Boolean? =
        preferences.current(playerId)?.receivePrivateMessages

    override suspend fun mailSummary(ownerId: UUID): MailSummaryPortOutcome {
        val owner = runner.run {
            known.onlineById(ownerId)
                ?: known.knownById(ownerId)
        } ?: return MailSummaryPortOutcome.PlayerUnknown
        return when (val result = mail.summary(owner)) {
            is MailSummaryResult.Summary ->
                MailSummaryPortOutcome.Summary(result.total, result.unread)

            MailSummaryResult.StorageUnavailable ->
                MailSummaryPortOutcome.StorageUnavailable
        }
    }

    override suspend fun sendPlayerMail(
        senderId: UUID,
        targetId: UUID,
        body: String,
        expiresAfter: Duration?,
    ): MailSendPortOutcome {
        val sender = runner.run { known.knownById(senderId) }
            ?: return MailSendPortOutcome.Rejected
        val target = runner.run {
            known.onlineById(targetId)
                ?: known.knownById(targetId)
        } ?: return MailSendPortOutcome.TargetUnknown
        return mail.send(
            MailSender.Player(sender.id, sender.name),
            target.name,
            body,
            expiresAfter,
        ).toPortOutcome()
    }

    override suspend fun sendSystemMail(
        targetId: UUID,
        title: String?,
        body: String,
        deduplicationKey: String?,
    ): MailSendPortOutcome {
        val target = runner.run {
            known.onlineById(targetId)
                ?: known.knownById(targetId)
        } ?: return MailSendPortOutcome.TargetUnknown
        // The mailbox model has no title or deduplication field; delivery keeps the existing shape.
        return mail.send(
            MailSender.Console,
            target.name,
            body,
            null
        ).toPortOutcome()
    }

    override suspend fun broadcast(
        sourceId: String,
        body: String,
    ): AnnouncementPortOutcome =
        announcements.broadcast(
            sourceId,
            body
        ).toPortOutcome()

    override suspend fun broadcastInDimension(
        sourceId: String,
        dimensionId: String,
        body: String,
    ): AnnouncementPortOutcome =
        announcements.broadcastWorld(
            sourceId,
            dimensionId,
            body
        ).toPortOutcome()

}

private fun PrivateMessageOutcome.toPortOutcome(): PrivateMessagePortOutcome =
    when (this) {
        is PrivateMessageOutcome.Delivered -> PrivateMessagePortOutcome.Delivered(target.id)
        PrivateMessageOutcome.Disabled -> PrivateMessagePortOutcome.Disabled
        PrivateMessageOutcome.InvalidMessage -> PrivateMessagePortOutcome.InvalidBody
        PrivateMessageOutcome.SenderRequired -> PrivateMessagePortOutcome.SenderOffline
        PrivateMessageOutcome.SenderOffline -> PrivateMessagePortOutcome.SenderOffline
        is PrivateMessageOutcome.SenderRestricted -> PrivateMessagePortOutcome.SenderRestricted
        is PrivateMessageOutcome.TargetOffline -> PrivateMessagePortOutcome.RecipientUnavailable
        PrivateMessageOutcome.SelfTarget -> PrivateMessagePortOutcome.RecipientUnavailable
        PrivateMessageOutcome.NoReplyTarget -> PrivateMessagePortOutcome.RecipientUnavailable
        PrivateMessageOutcome.RecipientUnavailable -> PrivateMessagePortOutcome.RecipientUnavailable
        PrivateMessageOutcome.RecipientStateLoading -> PrivateMessagePortOutcome.RecipientUnavailable
    }

private fun MailSendResult.toPortOutcome(): MailSendPortOutcome =
    when (this) {
        is MailSendResult.Sent -> MailSendPortOutcome.Accepted
        MailSendResult.Disabled -> MailSendPortOutcome.Disabled
        MailSendResult.InvalidMessage -> MailSendPortOutcome.InvalidBody
        is MailSendResult.SenderRestricted -> MailSendPortOutcome.Rejected
        is MailSendResult.TargetUnknown -> MailSendPortOutcome.TargetUnknown
        MailSendResult.SelfTarget -> MailSendPortOutcome.Rejected
        MailSendResult.RateLimited -> MailSendPortOutcome.RateLimited
        MailSendResult.MailboxFull -> MailSendPortOutcome.MailboxFull
        MailSendResult.DurationTooLong -> MailSendPortOutcome.Rejected
        MailSendResult.StorageUnavailable -> MailSendPortOutcome.StorageUnavailable
    }

private fun AnnouncementResult.toPortOutcome(): AnnouncementPortOutcome =
    when (this) {
        is AnnouncementResult.Delivered -> AnnouncementPortOutcome.Delivered(recipientCount)
        AnnouncementResult.NoRecipients -> AnnouncementPortOutcome.NoRecipients
        AnnouncementResult.Disabled -> AnnouncementPortOutcome.Disabled
        AnnouncementResult.InvalidMessage -> AnnouncementPortOutcome.InvalidBody
    }
