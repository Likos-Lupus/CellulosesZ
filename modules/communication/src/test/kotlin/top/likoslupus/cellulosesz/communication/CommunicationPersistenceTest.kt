package top.likoslupus.cellulosesz.communication

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import top.likoslupus.cellulosesz.communication.mail.JdbcMailboxRepository
import top.likoslupus.cellulosesz.communication.mail.MailMessage
import top.likoslupus.cellulosesz.communication.mail.MailSender
import top.likoslupus.cellulosesz.communication.mail.Mailbox
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.communication.preferences.JdbcMessagingPreferencesRepository
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferences
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.time.Instant
import java.util.*

class CommunicationPersistenceTest {

    @TempDir
    lateinit var directory: Path

    private fun runtime(): HikariDatabaseRuntime {
        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(
                sqlite = SqliteSettings(
                    path = directory.resolve("cz.db").toString()
                )
            ),
            directory,
        )
        val runtime = HikariDatabaseRuntime.create(descriptor)
        SchemaInitializer.initializeBlocking(
            runtime,
            listOf(CommunicationSqlSchema)
        )
        return runtime
    }

    @Test
    fun `preferences round-trip including ignores and reply mode`() =
        runBlocking {
            runtime().use {
                val repository = JdbcMessagingPreferencesRepository(
                    it,
                    "default"
                )
                val player = UUID.randomUUID()
                val ignored = UUID.randomUUID()

                assertNull(repository.load(player))
                repository.save(
                    player,
                    "Alice",
                    MessagingPreferences(
                        receivePrivateMessages = false,
                        ignoredPlayerIds = setOf(ignored),
                        replyMode = ReplyMode.LAST_INCOMING,
                    ),
                )

                val loaded = checkNotNull(repository.load(player))
                assertEquals(false, loaded.receivePrivateMessages)
                assertEquals(setOf(ignored), loaded.ignoredPlayerIds)
                assertEquals(ReplyMode.LAST_INCOMING, loaded.replyMode)
                assertTrue(repository.delete(player))
                assertNull(repository.load(player))
            }
        }

    @Test
    fun `mailbox round-trip preserves sender, order and read state`() =
        runBlocking {
            runtime().use { runtime ->
                val repository = JdbcMailboxRepository(runtime, "default")
                val owner = UUID.randomUUID()
                val sender = UUID.randomUUID()
                val message = MailMessage(
                    id = UUID.randomUUID(),
                    sender = MailSender.Player(sender, "Bob"),
                    body = checkNotNull(MessageBody.parse("hello", 1000)),
                    sentAt = Instant.ofEpochMilli(1_000),
                    expiresAt = null,
                    readAt = Instant.ofEpochMilli(2_000),
                )

                assertNull(repository.load(owner))
                repository.save(
                    Mailbox(
                        ownerId = owner,
                        ownerName = "Alice",
                        messages = listOf(message)
                    )
                )

                val loaded = checkNotNull(repository.load(owner))
                assertEquals(1, loaded.messages.size)
                assertEquals(
                    "Bob",
                    loaded.messages.single().sender.let {
                        (it as MailSender.Player).name
                    }
                )
                assertEquals(
                    Instant.ofEpochMilli(2_000),
                    loaded.messages.single().readAt
                )
                assertTrue(repository.delete(owner))
                assertNull(repository.load(owner))
            }
        }

}
