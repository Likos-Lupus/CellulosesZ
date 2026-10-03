package top.likoslupus.cellulosesz.communication.mail

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import java.time.Instant
import java.util.*

internal const val MAILBOX_SCHEMA_VERSION: Int = 1

/** Machine safety caps, deliberately far above the configurable product limits. */
internal const val MAX_STORED_MAIL_MESSAGES: Int = 10_000
internal const val MAX_STORED_MESSAGE_CHARS: Int = 16_384

@Serializable
internal data class MailMessageFile(
    val id: String,
    val senderType: String,
    val senderId: String? = null,
    val senderName: String? = null,
    val body: String,
    val sentAtEpochMillis: Long,
    val expiresAtEpochMillis: Long? = null,
    val readAtEpochMillis: Long? = null,
)

@Serializable
internal data class MailboxFile(
    val schemaVersion: Int = MAILBOX_SCHEMA_VERSION,
    val ownerId: String,
    val ownerName: String,
    val messages: List<MailMessageFile> = emptyList(),
)

internal fun MailMessage.toFile(): MailMessageFile =
    when (val writer = sender) {
        MailSender.Console -> MailMessageFile(
            id = id.toString(),
            senderType = CONSOLE,
            body = body.value,
            sentAtEpochMillis = sentAt.toEpochMilli(),
            expiresAtEpochMillis = expiresAt?.toEpochMilli(),
            readAtEpochMillis = readAt?.toEpochMilli(),
        )

        is MailSender.Player -> MailMessageFile(
            id = id.toString(),
            senderType = PLAYER,
            senderId = writer.id.toString(),
            senderName = writer.name,
            body = body.value,
            sentAtEpochMillis = sentAt.toEpochMilli(),
            expiresAtEpochMillis = expiresAt?.toEpochMilli(),
            readAtEpochMillis = readAt?.toEpochMilli(),
        )
    }

internal fun MailMessageFile.toDomain(): MailMessage {
    val parsedId = try {
        UUID.fromString(id)
    } catch (_: IllegalArgumentException) {
        throw MailboxDataException("mail message has an invalid id '$id'")
    }

    val parsedSender = when (senderType) {
        CONSOLE -> MailSender.Console
        PLAYER -> {
            val id = senderId
            val name = senderName
            if (id == null || name == null) {
                throw MailboxDataException("player mail message is missing its sender identity")
            }

            val senderUuid = try {
                UUID.fromString(id)
            } catch (_: IllegalArgumentException) {
                throw MailboxDataException("mail message has an invalid sender id '$id'")
            }

            MailSender.Player(senderUuid, name)
        }

        else -> throw MailboxDataException("mail message has an unknown sender type '$senderType'")
    }

    if (body.length > MAX_STORED_MESSAGE_CHARS) {
        throw MailboxDataException("mail message body exceeds the storage cap")
    }

    val parsedBody = MessageBody.parse(body, MAX_STORED_MESSAGE_CHARS)
        ?: throw MailboxDataException("mail message body is not a valid message body")

    return MailMessage(
        id = parsedId,
        sender = parsedSender,
        body = parsedBody,
        sentAt = Instant.ofEpochMilli(sentAtEpochMillis),
        expiresAt = expiresAtEpochMillis?.let(Instant::ofEpochMilli),
        readAt = readAtEpochMillis?.let(Instant::ofEpochMilli),
    )
}

internal fun Mailbox.toFile(): MailboxFile =
    MailboxFile(
        ownerId = ownerId.toString(),
        ownerName = ownerName,
        messages = messages.map { it.toFile() },
    )

internal fun MailboxFile.toDomain(): Mailbox =
    Mailbox(
        ownerId = UUID.fromString(ownerId),
        ownerName = ownerName,
        messages = messages.map { it.toDomain() },
    )

private const val PLAYER: String = "player"
private const val CONSOLE: String = "console"
