package top.likoslupus.cellulosesz.administration.moderation.mute

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

class FileMuteRepositoryTest {

    @TempDir
    lateinit var root: Path

    private fun repository(): FileMuteRepository =
        FileMuteRepository { root }

    private fun mute(
        id: UUID,
        issuedAt: Instant = Instant.ofEpochMilli(1_000L),
        expiresAt: Instant? = null,
    ): Mute =
        Mute(
            playerId = id,
            playerName = "Alice",
            actor = PersistedModerationActor(
                "console",
                null,
                "console"
            ),
            reason = "spam",
            issuedAt = issuedAt,
            expiresAt = expiresAt,
        )

    private fun mutesDir(): Path = root
            .resolve("moderation")
            .resolve("mutes")

    @Test
    fun `round trips and removes a record`() =
        runBlocking {
            val id = UUID.randomUUID()
            val repository = repository()

            repository.put(mute(id))
            val loaded = repository.loadAll()
            assertEquals(
                listOf(id),
                loaded.records.map { it.playerId }
            )
            assertTrue(loaded.corruptPlayerIds.isEmpty())

            assertTrue(repository.remove(id))
            assertTrue(repository.loadAll().records.isEmpty())
            assertFalse(repository.remove(id))
        }

    @Test
    fun `corrupt json is isolated and not overwritten`() =
        runBlocking {
            Files.createDirectories(mutesDir())
            val id = UUID.randomUUID()
            val file = mutesDir().resolve("$id.json")
            Files.writeString(file, "{ this is not valid json")

            val loaded = repository().loadAll()
            assertTrue(loaded.records.isEmpty())
            assertEquals(
                setOf(id),
                loaded.corruptPlayerIds
            )
            assertEquals(
                "{ this is not valid json",
                Files.readString(file)
            )
        }

    @Test
    fun `filename that does not match playerId is rejected`() =
        runBlocking {
            Files.createDirectories(mutesDir())
            val id = UUID.randomUUID()
            val other = UUID.randomUUID()
            val file = mutesDir().resolve("$id.json")
            Files.writeString(
                file,
                StorageJson.format.encodeToString(
                    MuteFile.serializer(),
                    MuteFile(
                        playerId = other.toString(),
                        playerName = "Alice",
                        actor = PersistedModerationActor(
                            "console",
                            null,
                            "console"
                        ),
                        reason = "spam",
                        issuedAtEpochMillis = 1_000L,
                    ),
                ),
            )

            assertEquals(
                setOf(id),
                repository().loadAll().corruptPlayerIds
            )
        }

    @Test
    fun `unsupported schema is rejected`() =
        runBlocking {
            Files.createDirectories(mutesDir())
            val id = UUID.randomUUID()
            Files.writeString(
                mutesDir().resolve("$id.json"),
                StorageJson.format.encodeToString(
                    MuteFile.serializer(),
                    MuteFile(
                        schemaVersion = 99,
                        playerId = id.toString(),
                        playerName = "Alice",
                        actor = PersistedModerationActor(
                            "console",
                            null,
                            "console"
                        ),
                        reason = "spam",
                        issuedAtEpochMillis = 1_000L,
                    ),
                ),
            )

            assertEquals(
                setOf(id),
                repository().loadAll().corruptPlayerIds
            )
        }

    @Test
    fun `expiry not after issue time is rejected`() =
        runBlocking {
            Files.createDirectories(mutesDir())
            val id = UUID.randomUUID()
            Files.writeString(
                mutesDir().resolve("$id.json"),
                StorageJson.format.encodeToString(
                    MuteFile.serializer(),
                    MuteFile(
                        playerId = id.toString(),
                        playerName = "Alice",
                        actor = PersistedModerationActor(
                            "console",
                            null,
                            "console"
                        ),
                        reason = "spam",
                        issuedAtEpochMillis = 5_000L,
                        expiresAtEpochMillis = 1_000L,
                    ),
                ),
            )

            assertEquals(
                setOf(id),
                repository().loadAll().corruptPlayerIds
            )
        }

}
