package top.likoslupus.cellulosesz.communication.mail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.communication.CommunicationSqlSchema
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.util.*

internal class JdbcMailboxRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : MailboxRepository {

    override suspend fun load(ownerId: UUID): Mailbox? =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    val ownerName = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            owner_name
                        FROM
                            ${CommunicationSqlSchema.MAILBOXES}
                        WHERE
                            namespace = ? AND owner_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, ownerId.toString())
                        statement.executeQuery()
                                .use { rs ->
                                    when {
                                        rs.next() -> rs.getString(1)
                                        else -> null
                                    }
                                }
                    } ?: return@read null

                    val messages = connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            message_id,
                            sender_type,
                            sender_uuid,
                            sender_name,
                            body,
                            sent_at_ms,
                            expires_at_ms,
                            read_at_ms
                        FROM
                            ${CommunicationSqlSchema.MAIL_MESSAGES}
                        WHERE
                            namespace = ? AND owner_uuid = ?
                        ORDER BY
                            sent_at_ms;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, ownerId.toString())
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        MailMessage(
                                            id = parseUuid(
                                                rs.getString(1),
                                                "message id"
                                            ),
                                            sender = parseSender(
                                                rs.getString(2),
                                                rs.getString(3),
                                                rs.getString(4)
                                            ),
                                            body = parseBody(rs.getString(5)),
                                            sentAt = Instant.ofEpochMilli(rs.getLong(6)),
                                            expiresAt = rs.getObject(7)
                                                    ?.let { Instant.ofEpochMilli(rs.getLong(7)) },
                                            readAt = rs.getObject(8)
                                                    ?.let { Instant.ofEpochMilli(rs.getLong(8)) },
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (messages.size > CommunicationSqlSchema.MAX_STORED_MAIL_MESSAGES) {
                        throw MailboxDataException("stored mailbox exceeds the message cap")
                    }

                    Mailbox(ownerId, ownerName, messages)
                }
            } catch (exception: SQLException) {
                throw MailboxDataException("unable to read mailbox for $ownerId", exception)
            }
        }

    override suspend fun save(mailbox: Mailbox) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${CommunicationSqlSchema.MAILBOXES}
                        WHERE
                            namespace = ? AND owner_uuid = ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, mailbox.ownerId.toString())
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${CommunicationSqlSchema.MAILBOXES} (
                            namespace,
                            owner_uuid,
                            owner_name
                        )
                        VALUES (
                            ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, mailbox.ownerId.toString())
                        statement.setString(3, mailbox.ownerName)
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${CommunicationSqlSchema.MAIL_MESSAGES}
                        WHERE
                            namespace = ? AND owner_uuid = ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, mailbox.ownerId.toString())
                        statement.executeUpdate()
                    }
                    if (mailbox.messages.isNotEmpty()) {
                        connection.prepareStatement(
                            /* language=SQL */ """
                            INSERT INTO ${CommunicationSqlSchema.MAIL_MESSAGES} (
                                namespace,
                                message_id,
                                owner_uuid,
                                sender_type,
                                sender_uuid,
                                sender_name,
                                body,
                                sent_at_ms,
                                expires_at_ms,
                                read_at_ms
                            )
                            VALUES (
                                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                            );
                            """.trimIndent()
                        ).use { statement ->
                            mailbox.messages.forEach { message ->
                                statement.setString(1, namespace)
                                statement.setString(2, message.id.toString())
                                statement.setString(3, mailbox.ownerId.toString())
                                when (val sender = message.sender) {
                                    MailSender.Console -> {
                                        statement.setString(4, SENDER_CONSOLE)
                                        statement.setNull(5, Types.VARCHAR)
                                        statement.setNull(6, Types.VARCHAR)
                                    }

                                    is MailSender.Player -> {
                                        statement.setString(4, SENDER_PLAYER)
                                        statement.setString(5, sender.id.toString())
                                        statement.setString(6, sender.name)
                                    }
                                }
                                statement.setString(7, message.body.value)
                                statement.setLong(8, message.sentAt.toEpochMilli())
                                message.expiresAt?.let { statement.setLong(9, it.toEpochMilli()) }
                                    ?: statement.setNull(9, Types.BIGINT)
                                message.readAt?.let { statement.setLong(10, it.toEpochMilli()) }
                                    ?: statement.setNull(10, Types.BIGINT)
                                statement.addBatch()
                            }
                            statement.executeBatch()
                        }
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw MailboxDataException(
                    "unable to write mailbox for ${mailbox.ownerId}",
                    exception
                )
            }
        }

    override suspend fun delete(ownerId: UUID): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${CommunicationSqlSchema.MAIL_MESSAGES}
                        WHERE
                            namespace = ? AND owner_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, ownerId.toString())
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${CommunicationSqlSchema.MAILBOXES}
                        WHERE
                            namespace = ? AND owner_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, ownerId.toString())
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw MailboxDataException("unable to delete mailbox for $ownerId", exception)
            }
        }

    private fun parseSender(
        type: String,
        senderId: String?,
        senderName: String?
    ): MailSender =
        when (type) {
            SENDER_CONSOLE -> MailSender.Console
            SENDER_PLAYER -> {
                if (senderId == null || senderName == null) {
                    throw MailboxDataException("player mail message is missing its sender identity")
                }
                MailSender.Player(
                    parseUuid(
                        senderId,
                        "sender id"
                    ), senderName
                )
            }

            else -> throw MailboxDataException("unknown mail sender type '$type'")
        }

    private fun parseBody(raw: String): MessageBody {
        if (raw.length > CommunicationSqlSchema.MAX_STORED_MESSAGE_CHARS) {
            throw MailboxDataException("mail body exceeds the storage cap")
        }
        return MessageBody.parse(raw, CommunicationSqlSchema.MAX_STORED_MESSAGE_CHARS)
            ?: throw MailboxDataException("mail body is not a valid message body")
    }

    private fun parseUuid(raw: String, label: String): UUID =
        try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            throw MailboxDataException("invalid $label UUID '$raw'")
        }

    private companion object {

        const val SENDER_PLAYER: String = "player"
        const val SENDER_CONSOLE: String = "console"

    }

}
