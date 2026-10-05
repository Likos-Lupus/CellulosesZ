package top.likoslupus.cellulosesz.communication.mail

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

class FileMailboxRepositoryTest {

    private val root: Path = Files.createTempDirectory("cellulosesz-mail")
    private val repository = FileMailboxRepository { root }
    private val owner = KnownPlayerIdentity(UUID.randomUUID(), "Owner")
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `missing file loads null`() = runBlocking {
        assertNull(repository.load(owner.id))
    }

    @Test
    fun `round trip preserves messages`() =
        runBlocking {
            val message = MailMessage(
                id = UUID.randomUUID(),
                sender = MailSender.Player(UUID.randomUUID(), "Alice"),
                body = MessageBody.parse("hello", 64)!!,
                sentAt = now,
                expiresAt = now.plusSeconds(60),
                readAt = null,
            )
            val mailbox = Mailbox.empty(owner) + message

            repository.save(mailbox)

            assertEquals(
                mailbox,
                repository.load(owner.id)
            )
        }

    @Test
    fun `corrupt file is not overwritten`() =
        runBlocking {
            val file = root
                    .resolve("mail")
                    .resolve("${owner.id}.json")
            Files.createDirectories(file.parent)
            Files.writeString(file, "{ not valid json")

            assertThrows(MailboxDataException::class.java) {
                runBlocking { repository.load(owner.id) }
            }
            assertTrue(Files.exists(file))
        }

    @Test
    fun `future schema is rejected`() =
        runBlocking {
            val file = root
                    .resolve("mail")
                    .resolve("${owner.id}.json")
            Files.createDirectories(file.parent)
            Files.writeString(
                file,
                /* language=JSON */ """
                {
                  "schemaVersion": 99,
                  "ownerId": "${owner.id}",
                  "ownerName": "Owner",
                  "messages": []
                }
                """.trimIndent()
            )

            assertThrows(MailboxDataException::class.java) {
                runBlocking { repository.load(owner.id) }
            }
            Unit
        }

}
