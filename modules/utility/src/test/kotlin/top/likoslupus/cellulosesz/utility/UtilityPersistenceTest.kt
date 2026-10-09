package top.likoslupus.cellulosesz.utility

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import top.likoslupus.cellulosesz.utility.kit.*
import java.nio.file.Path
import java.time.Instant
import java.util.*

class UtilityPersistenceTest {

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
        SchemaInitializer.initializeBlocking(runtime, listOf(UtilitySqlSchema))
        return runtime
    }

    @Test
    fun `kit catalog create replace remove`() =
        runBlocking {
            runtime().use { runtime ->
                val codec = KitItemCodec { error("item codec must not be used for empty kits") }
                val repository = JdbcKitRepository(runtime, "default", codec)
                val name = checkNotNull(KitName.parse("starter"))
                val definition = KitDefinition(
                    KitId(UUID.randomUUID()),
                    name,
                    KitReusePolicy.Once,
                    emptyList()
                )

                assertTrue(repository.create(definition))
                assertFalse(repository.create(definition))
                assertEquals(1, repository.loadAll().size)
                assertTrue(repository.replace(definition))
                assertTrue(repository.remove(name))
                assertTrue(repository.loadAll().isEmpty())
            }
        }

    @Test
    fun `kit claims reserve then deliver`() =
        runBlocking {
            runtime().use { runtime ->
                val repository = JdbcKitClaimRepository(runtime, "default")
                val player = KnownPlayerIdentity(UUID.randomUUID(), "Alice")
                val name = checkNotNull(KitName.parse("starter"))
                val kit = KitDefinition(
                    KitId(UUID.randomUUID()),
                    name,
                    KitReusePolicy.Once,
                    emptyList()
                )

                repository.reserve(
                    player,
                    kit,
                    Instant.ofEpochMilli(1_000)
                )
                assertEquals(
                    KitClaimStatus.RESERVED,
                    repository.load(player.id).getValue(name).status
                )
                repository.markDelivered(
                    player,
                    kit,
                    Instant.ofEpochMilli(2_000)
                )
                assertEquals(
                    KitClaimStatus.DELIVERED,
                    repository.load(player.id).getValue(name).status
                )
                assertTrue(repository.reset(player.id, name))
                assertTrue(repository.load(player.id).isEmpty())
            }
        }

}
