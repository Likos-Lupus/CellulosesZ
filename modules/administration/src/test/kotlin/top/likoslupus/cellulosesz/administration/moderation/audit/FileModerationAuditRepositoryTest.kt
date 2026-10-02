package top.likoslupus.cellulosesz.administration.moderation.audit

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

class FileModerationAuditRepositoryTest {

    @TempDir
    lateinit var root: Path

    private fun entry(id: String, occurredAt: Instant): ModerationAuditFile =
        ModerationAuditFile(
            id = id,
            occurredAtEpochMillis = occurredAt.toEpochMilli(),
            actor = PersistedModerationActor("console", null, "console"),
            action = ModerationAuditAction.BAN,
            target = PersistedModerationTarget.account(
                PlayerIdentity(
                    UUID.randomUUID(),
                    "Alice"
                )
            ),
            reason = "griefing",
        )

    private fun expectedFile(id: String, occurredAt: Instant): Path {
        val day = LocalDate.ofInstant(occurredAt, ZoneOffset.UTC)
        return root
                .resolve("moderation")
                .resolve("audit")
                .resolve(day.toString())
                .resolve("${occurredAt.toEpochMilli()}-$id.json")
    }

    @Test
    fun `writes one immutable record per entry`() =
        runBlocking {
            val repository = FileModerationAuditRepository { root }
            val occurredAt = Instant.parse("2026-10-01T12:00:00Z")
            val record = entry("abc", occurredAt)

            repository.append(record)

            val file = expectedFile("abc", occurredAt)
            assertTrue(Files.exists(file))
            val decoded = StorageJson.format.decodeFromString(
                ModerationAuditFile.serializer(),
                Files.readString(file),
            )
            assertEquals(record, decoded)
        }

    @Test
    fun `two records in the same millisecond do not collide`() =
        runBlocking {
            val repository = FileModerationAuditRepository { root }
            val occurredAt = Instant.parse("2026-10-01T12:00:00Z")

            repository.append(entry("one", occurredAt))
            repository.append(entry("two", occurredAt))

            assertTrue(Files.exists(expectedFile("one", occurredAt)))
            assertTrue(Files.exists(expectedFile("two", occurredAt)))
        }

}
