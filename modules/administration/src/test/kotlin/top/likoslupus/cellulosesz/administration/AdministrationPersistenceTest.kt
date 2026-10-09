package top.likoslupus.cellulosesz.administration

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.audit.*
import top.likoslupus.cellulosesz.administration.moderation.mute.JdbcMuteRepository
import top.likoslupus.cellulosesz.administration.moderation.mute.Mute
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.time.Instant
import java.util.*

class AdministrationPersistenceTest {

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
    fun `mutes round-trip`() =
        runBlocking {
            runtime().use {
                val repository = JdbcMuteRepository(it, "default")
                val player = UUID.randomUUID()
                val mute = Mute(
                    playerId = player,
                    playerName = "Alice",
                    actor = PersistedModerationActor.of(ModerationActor.Console),
                    reason = "spam",
                    issuedAt = Instant.ofEpochMilli(1_000),
                    expiresAt = Instant.ofEpochMilli(9_000),
                )

                repository.put(mute)
                val loaded = repository.loadAll()
                assertEquals(1, loaded.records.size)
                assertEquals(
                    Instant.ofEpochMilli(9_000),
                    loaded.records.single().expiresAt
                )
                assertTrue(loaded.corruptPlayerIds.isEmpty())
                assertTrue(repository.remove(player))
                assertTrue(repository.loadAll().records.isEmpty())
            }
        }

    @Test
    fun `audit append writes a header and its details`() =
        runBlocking {
            runtime().use {
                val repository = JdbcModerationAuditRepository(it, "default")
                repository.append(
                    ModerationAuditFile(
                        id = UUID.randomUUID().toString(),
                        occurredAtEpochMillis = 5_000,
                        actor = PersistedModerationActor.of(ModerationActor.Console),
                        action = ModerationAuditAction.BAN,
                        target = PersistedModerationTarget.ip("1.2.3.4"),
                        reason = "x",
                        expiresAtEpochMillis = null,
                        details = mapOf("duration" to "1h"),
                    )
                )

                val counts = it.read { connection ->
                    val headers = connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT COUNT(*) FROM ${AdministrationSqlSchema.MODERATION_AUDIT}"
                        ).use { rs ->
                            rs.next()
                            rs.getInt(1)
                        }
                    }
                    val details = connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT COUNT(*) FROM ${AdministrationSqlSchema.MODERATION_AUDIT_DETAILS}"
                        ).use { rs ->
                            rs.next()
                            rs.getInt(1)
                        }
                    }
                    headers to details
                }

                assertEquals(1, counts.first)
                assertEquals(1, counts.second)
            }
        }

}
