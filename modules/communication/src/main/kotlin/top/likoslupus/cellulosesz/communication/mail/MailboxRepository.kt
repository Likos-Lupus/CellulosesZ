package top.likoslupus.cellulosesz.communication.mail

import java.util.*

internal interface MailboxRepository {

    /** Returns `null` when the owner has no stored mailbox yet. */
    suspend fun load(ownerId: UUID): Mailbox?

    suspend fun save(mailbox: Mailbox)

    suspend fun delete(ownerId: UUID): Boolean

}

internal class MailboxDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(
    message,
    cause,
)
