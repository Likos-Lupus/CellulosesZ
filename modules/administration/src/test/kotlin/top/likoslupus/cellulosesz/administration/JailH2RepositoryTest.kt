package top.likoslupus.cellulosesz.administration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.administration.jail.*
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking

/** The jail schema is backend-neutral; verify the online-time columns survive H2. */
class JailH2RepositoryTest {

    @TempDir
    lateinit var directory: Path

    private fun runtime(): HikariDatabaseRuntime {
        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(
                type = DatabaseType.H2,
                h2 = H2Settings(path = directory.resolve("czh2").toString()),
            ),
            directory,
        )
        val runtime = HikariDatabaseRuntime.create(descriptor)
        SchemaInitializer.initializeBlocking(
            runtime,
            listOf(AdministrationSqlSchema)
        )
        return runtime
    }

    @Test
    fun `online sentence round-trips on H2`() =
        runBlocking {
            runtime().use {
                val repository = JdbcJailSentenceRepository(it, "default")
                val player = UUID.randomUUID()
                val jail = checkNotNull(JailName.parse("cells"))
                repository.put(
                    JailSentence(
                        playerId = player,
                        playerName = "Alice",
                        jailName = jail,
                        status = JailStatus.ACTIVE,
                        actor = PersistedModerationActor.of(ModerationActor.Console),
                        reason = "griefing",
                        issuedAt = Instant.ofEpochMilli(1_000),
                        mode = JailSentenceMode.ONLINE,
                        expiresAt = null,
                        remainingSeconds = 600L,
                        returnPosition = null,
                        releaseRequestedAt = null,
                    )
                )

                val loaded = repository.loadAll().single()
                assertEquals(JailSentenceMode.ONLINE, loaded.mode)
                assertEquals(600L, loaded.remainingSeconds)

                assertTrue(repository.updateRemaining(player, 15L))
                assertEquals(
                    15L,
                    repository.loadAll().single().remainingSeconds
                )
            }
        }

}
