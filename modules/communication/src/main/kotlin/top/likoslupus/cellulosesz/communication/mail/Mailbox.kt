package top.likoslupus.cellulosesz.communication.mail

import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import java.time.Instant
import java.util.*

/** A bounded, immutable collection of mail for one owner. */
internal data class Mailbox(
    val ownerId: UUID,
    val ownerName: String,
    val messages: List<MailMessage>,
) {

    val unreadCount: Int get() = messages.count { it.readAt == null }

    fun pruneExpired(now: Instant): Mailbox {
        val kept = messages.filterNot { it.isExpired(now) }
        return when (kept.size == messages.size) {
            true -> this
            false -> copy(messages = kept)
        }
    }

    operator fun plus(message: MailMessage): Mailbox =
        copy(messages = messages + message)

    fun markRead(ids: Set<UUID>, now: Instant): Mailbox =
        copy(
            messages = messages.map { message ->
                when {
                    message.id in ids && message.readAt == null -> message.copy(readAt = now)
                    else -> message
                }
            }
        )

    fun clear(): Mailbox =
        copy(messages = emptyList())

    /** Newest first, the single fixed presentation order. */
    fun newestFirst(): List<MailMessage> =
        messages.sortedByDescending { it.sentAt }

    companion object {

        fun empty(ownerId: UUID, ownerName: String): Mailbox =
            Mailbox(
                ownerId,
                ownerName,
                emptyList()
            )

        fun empty(owner: KnownPlayerIdentity): Mailbox =
            Mailbox(
                owner.id,
                owner.name,
                emptyList()
            )

    }

}

private fun MailMessage.isExpired(now: Instant): Boolean =
    expiresAt?.let { !it.isAfter(now) } == true
