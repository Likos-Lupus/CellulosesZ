package top.likoslupus.cellulosesz.administration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.administration.jail.*
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.core.player.WorldPosition
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking

class JailRepositoryTest {

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
            listOf(AdministrationSqlSchema)
        )
        return runtime
    }

    @Test
    fun `jail locations round-trip and are namespaced`() =
        runBlocking {
            runtime().use {
                val repository = JdbcJailLocationRepository(it, "default")
                val name = checkNotNull(JailName.parse("cells"))
                val location = JailLocation(
                    name = name,
                    dimension = "minecraft:overworld",
                    x = 10.5,
                    y = 64.0,
                    z = -3.25,
                    yaw = 90f,
                    pitch = 15f,
                    radius = 8.0,
                    createdAt = Instant.ofEpochMilli(2_000),
                    createdBy = "Admin",
                )

                repository.put(location)
                val loaded = repository.loadAll()
                assertEquals(1, loaded.size)
                assertEquals(location, loaded.single())
                assertTrue(repository.remove(name))
                assertTrue(repository.loadAll().isEmpty())
            }
        }

    @Test
    fun `jail sentence transitions active to release pending to removed`() =
        runBlocking {
            runtime().use {
                val repository = JdbcJailSentenceRepository(it, "default")
                val player = UUID.randomUUID()
                val jail = checkNotNull(JailName.parse("cells"))
                val sentence = JailSentence(
                    playerId = player,
                    playerName = "Alice",
                    jailName = jail,
                    status = JailStatus.ACTIVE,
                    actor = PersistedModerationActor.of(ModerationActor.Console),
                    reason = "griefing",
                    issuedAt = Instant.ofEpochMilli(1_000),
                    mode = JailSentenceMode.WALL_CLOCK,
                    expiresAt = Instant.ofEpochMilli(9_000),
                    remainingSeconds = null,
                    returnPosition = WorldPosition(
                        dimension = "minecraft:overworld",
                        x = 1.5,
                        y = 64.0,
                        z = 2.5,
                        yaw = 0f,
                        pitch = 0f,
                    ),
                    releaseRequestedAt = null,
                )

                repository.put(sentence)
                assertEquals(
                    JailStatus.ACTIVE,
                    repository.loadAll().single().status
                )
                assertEquals(
                    Instant.ofEpochMilli(9_000),
                    repository.loadAll().single().expiresAt
                )
                assertEquals(
                    WorldPosition(
                        dimension = "minecraft:overworld",
                        x = 1.5,
                        y = 64.0,
                        z = 2.5,
                        yaw = 0f,
                        pitch = 0f,
                    ),
                    repository.loadAll().single().returnPosition,
                )

                assertTrue(
                    repository.updateStatus(
                        player,
                        JailStatus.RELEASE_PENDING,
                        Instant.ofEpochMilli(3_000),
                    )
                )
                val pending = repository.loadAll().single()
                assertEquals(
                    JailStatus.RELEASE_PENDING,
                    pending.status
                )
                assertEquals("griefing", pending.reason)
                assertEquals(
                    Instant.ofEpochMilli(3_000),
                    pending.releaseRequestedAt,
                )

                assertTrue(repository.remove(player))
                assertFalse(repository.loadAll().any { it.playerId == player })
            }
        }

    @Test
    fun `online sentence round-trips its mode and remaining seconds`() =
        runBlocking {
            runtime().use {
                val repository = JdbcJailSentenceRepository(it, "default")
                val player = UUID.randomUUID()
                val jail = checkNotNull(JailName.parse("cells"))
                val sentence = JailSentence(
                    playerId = player,
                    playerName = "Alice",
                    jailName = jail,
                    status = JailStatus.ACTIVE,
                    actor = PersistedModerationActor.of(ModerationActor.Console),
                    reason = "griefing",
                    issuedAt = Instant.ofEpochMilli(1_000),
                    mode = JailSentenceMode.ONLINE,
                    expiresAt = null,
                    remainingSeconds = 300L,
                    returnPosition = null,
                    releaseRequestedAt = null,
                )

                repository.put(sentence)
                val loaded = repository.loadAll().single()
                assertEquals(JailSentenceMode.ONLINE, loaded.mode)
                assertEquals(300L, loaded.remainingSeconds)
                assertNull(loaded.expiresAt)

                assertTrue(repository.updateRemaining(player, 120L))
                val updated = repository.loadAll().single()
                assertEquals(120L, updated.remainingSeconds)
                assertEquals(JailSentenceMode.ONLINE, updated.mode)
            }
        }

}
