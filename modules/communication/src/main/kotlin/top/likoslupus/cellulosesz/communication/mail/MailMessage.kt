package top.likoslupus.cellulosesz.communication.mail

import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import java.time.Instant
import java.util.*

internal data class MailMessage(
    val id: UUID,
    val sender: MailSender,
    val body: MessageBody,
    val sentAt: Instant,
    val expiresAt: Instant?,
    val readAt: Instant?,
)
