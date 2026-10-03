package top.likoslupus.cellulosesz.communication.mail

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import java.time.Instant
import java.util.*

class MailboxTest {

    private val owner = KnownPlayerIdentity(UUID.randomUUID(), "Owner")
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun message(
        id: UUID = UUID.randomUUID(),
        sentAt: Instant = now,
        expiresAt: Instant? = null,
        readAt: Instant? = null,
    ) =
        MailMessage(
            id = id,
            sender = MailSender.Console,
            body = MessageBody.parse("hello", 64)!!,
            sentAt = sentAt,
            expiresAt = expiresAt,
            readAt = readAt,
        )

    @Test
    fun `prunes expired messages only`() {
        val expired = message(expiresAt = now.minusSeconds(1))
        val alive = message(expiresAt = now.plusSeconds(60))
        val box = Mailbox.empty(owner) + expired + alive

        val pruned = box.pruneExpired(now)

        assertEquals(listOf(alive), pruned.messages)
    }

    @Test
    fun `prunes messages expiring exactly now`() {
        val borderline = message(expiresAt = now)
        val box = Mailbox.empty(owner) + borderline

        assertTrue(box.pruneExpired(now).messages.isEmpty())
    }

    @Test
    fun `newest first ordering`() {
        val older = message(sentAt = now.minusSeconds(10))
        val newer = message(sentAt = now)
        val box = Mailbox.empty(owner) + older + newer

        assertEquals(
            listOf(newer, older),
            box.newestFirst()
        )
    }

    @Test
    fun `mark read only affects unread matching ids`() {
        val target = message()
        val other = message()
        val box = Mailbox.empty(owner) + target + other

        val updated = box.markRead(setOf(target.id), now)

        assertEquals(
            now,
            updated.messages.first { it.id == target.id }.readAt
        )
        assertEquals(
            null,
            updated.messages.first { it.id == other.id }.readAt
        )
        assertEquals(
            1,
            updated.unreadCount
        )
    }

    @Test
    fun `clear empties the mailbox`() {
        val box = Mailbox.empty(owner) + message() + message()

        assertTrue(box.clear().messages.isEmpty())
    }

}
