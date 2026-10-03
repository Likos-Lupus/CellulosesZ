package top.likoslupus.cellulosesz.communication.mail

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.FakeTimeSource
import top.likoslupus.cellulosesz.communication.ImmediateServerThreadRunner
import top.likoslupus.cellulosesz.communication.SendGateResult
import top.likoslupus.cellulosesz.communication.config.MailSettings
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferences
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferencesRepository
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class MailServiceTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val mailboxes = InMemoryMailboxRepository()
    private val preferences = InMemoryPreferencesRepository()
    private val identities = FakeKnownResolver()
    private val sender = UUID.randomUUID()
    private val target = KnownPlayerIdentity(UUID.randomUUID(), "Bob")
    private var settings = MailSettings()
    private var gate: (UUID) -> SendGateResult = { SendGateResult.Allowed }
    private val rateLimiter = MailRateLimiter(FakeTimeSource())

    private val mail = MailService(
        runner = ImmediateServerThreadRunner,
        mailboxes = mailboxes,
        preferences = preferences,
        identities = identities,
        senderGate = { { id -> gate(id) } },
        rateLimiter = rateLimiter,
        clock = clock,
        settings = { settings },
        locks = KeyedMutex(),
    )

    private val playerSender = MailSender.Player(sender, "Alice")

    init {
        identities.known[target.name.lowercase()] = target
    }

    @Test
    fun `sends durable mail to a known offline player`() =
        runBlocking {
            val result = mail.send(
                playerSender,
                "Bob",
                "hello Bob"
            )

            assertTrue(result is MailSendResult.Sent)
            val stored = mailboxes.store.getValue(target.id)
            assertEquals(1, stored.messages.size)
            assertEquals("hello Bob", stored.messages.single().body.value)
        }

    @Test
    fun `unknown target is rejected`() =
        runBlocking {
            assertTrue(
                mail.send(
                    playerSender,
                    "Nobody",
                    "hi"
                ) is MailSendResult.TargetUnknown
            )
        }

    @Test
    fun `self mail rejected`() =
        runBlocking {
            identities.known["alice"] = KnownPlayerIdentity(sender, "Alice")

            assertTrue(
                mail.send(
                    playerSender,
                    "Alice",
                    "hi"
                ) is MailSendResult.SelfTarget
            )
        }

    @Test
    fun `rate limit is per sender`() =
        runBlocking {
            settings = settings.copy(maxSendsPerMinute = 1)

            assertTrue(
                mail.send(
                    playerSender,
                    "Bob",
                    "one"
                ) is MailSendResult.Sent
            )
            assertEquals(
                MailSendResult.RateLimited,
                mail.send(
                    playerSender,
                    "Bob",
                    "two"
                )
            )
        }

    @Test
    fun `mailbox capacity is enforced`() =
        runBlocking {
            settings = settings.copy(maxMessagesPerMailbox = 1)

            assertTrue(
                mail.send(
                    playerSender,
                    "Bob",
                    "one"
                ) is MailSendResult.Sent
            )
            assertEquals(
                MailSendResult.MailboxFull,
                mail.send(
                    playerSender,
                    "Bob",
                    "two"
                )
            )
        }

    @Test
    fun `ignored sender is silently accepted without storing`() =
        runBlocking {
            preferences.store[target.id] = MessagingPreferences(ignoredPlayerIds = setOf(sender))

            val result = mail.send(
                playerSender,
                "Bob",
                "hidden"
            )

            assertTrue(result is MailSendResult.Sent)
            assertFalse(mailboxes.store.containsKey(target.id))
        }

    @Test
    fun `temporary mail over the cap is rejected`() =
        runBlocking {
            settings = settings.copy(maxTemporaryMailSeconds = 60)

            val result = mail.send(
                playerSender,
                "Bob",
                "hi",
                Duration.ofSeconds(120)
            )

            assertEquals(
                MailSendResult.DurationTooLong,
                result
            )
        }

    @Test
    fun `storage failure is reported`() =
        runBlocking {
            mailboxes.failSaves = true

            assertEquals(
                MailSendResult.StorageUnavailable,
                mail.send(
                    playerSender,
                    "Bob",
                    "hi"
                )
            )
        }

    @Test
    fun `read marks displayed mail as read`() =
        runBlocking {
            seed(12)

            val view = mail.read(target, 1)

            assertTrue(view is MailViewResult.Loaded)
            view as MailViewResult.Loaded
            assertEquals(10, view.page.size)
            assertEquals(12, view.total)
            assertEquals(2, view.unread)
        }

    @Test
    fun `read paginates newest first`() =
        runBlocking {
            seed(12)

            val view = mail.read(target, 2) as MailViewResult.Loaded

            assertEquals(2, view.page.size)
            assertEquals(2, view.pageIndex)
            assertEquals(2, view.pageCount)
        }

    @Test
    fun `clear empties the mailbox`() =
        runBlocking {
            seed(3)

            assertTrue(mail.clear(target) is MailClearResult.Cleared)
            assertTrue(mailboxes.store.getValue(target.id).messages.isEmpty())
        }

    @Test
    fun `summary counts unread`() =
        runBlocking {
            seed(3)

            val summary = mail.summary(target) as MailSummaryResult.Summary

            assertEquals(3, summary.total)
            assertEquals(3, summary.unread)
        }

    private fun seed(count: Int) {
        var box = Mailbox.empty(target)
        repeat(count) {
            box += MailMessage(
                id = UUID.randomUUID(),
                sender = MailSender.Console,
                body = MessageBody.parse("message $it", 64)!!,
                sentAt = now.plusSeconds(it.toLong()),
                expiresAt = null,
                readAt = null,
            )
        }
        mailboxes.store[target.id] = box
    }

    private class InMemoryMailboxRepository : MailboxRepository {

        val store = HashMap<UUID, Mailbox>()
        var failSaves = false

        override suspend fun load(ownerId: UUID): Mailbox? =
            store[ownerId]

        override suspend fun save(mailbox: Mailbox) {
            if (failSaves) {
                throw MailboxDataException("disk full")
            }
            store[mailbox.ownerId] = mailbox
        }

        override suspend fun delete(ownerId: UUID): Boolean =
            store.remove(ownerId) != null

    }

    private class InMemoryPreferencesRepository : MessagingPreferencesRepository {

        val store = HashMap<UUID, MessagingPreferences>()

        override suspend fun load(playerId: UUID): MessagingPreferences? =
            store[playerId]

        override suspend fun save(
            playerId: UUID,
            playerName: String,
            preferences: MessagingPreferences,
        ) {
            store[playerId] = preferences
        }

        override suspend fun delete(playerId: UUID): Boolean =
            store.remove(playerId) != null

    }

    private class FakeKnownResolver : KnownPlayerResolver {

        val known = HashMap<String, KnownPlayerIdentity>()

        override fun onlineByName(name: String): KnownPlayerIdentity? =
            known[name.lowercase()]

        override fun onlineById(id: UUID): KnownPlayerIdentity? =
            known.values.firstOrNull { it.id == id }

        override fun knownByName(name: String): KnownPlayerIdentity? =
            known[name.lowercase()]

        override fun knownById(id: UUID): KnownPlayerIdentity? =
            known.values.firstOrNull { it.id == id }

    }

}
