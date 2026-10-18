package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.economy.worth.JdbcWorthRepository
import top.likoslupus.cellulosesz.economy.worth.WorthRecord
import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.runBlocking

/** `cz_economy_worth` against both supported local backends. */
class WorthBackendsTest {

    @TempDir
    lateinit var directory: Path

    private fun runtime(type: DatabaseType): HikariDatabaseRuntime {
        val descriptor = when (type) {
            DatabaseType.H2 -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    type = DatabaseType.H2,
                    h2 = H2Settings(path = directory.resolve("worth_h2").toString()),
                ),
                directory,
            )

            else -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    sqlite = SqliteSettings(
                        path = directory.resolve("worth_sqlite.db").toString()
                    )
                ),
                directory,
            )
        }
        val runtime = HikariDatabaseRuntime.create(descriptor)
        SchemaInitializer.initializeBlocking(runtime, listOf(EconomySqlSchema))
        return runtime
    }

    private fun record(
        itemId: String,
        minor: Long,
        by: String? = "Alex"
    ) =
        WorthRecord(
            itemId,
            minor,
            Instant.ofEpochMilli(minor + 1),
            by
        )

    @Test
    fun `upsert inserts then replaces and keeps updated_by on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val repository = JdbcWorthRepository(runtime, "default")

                    repository.upsert(record("minecraft:diamond", 5000L))
                    assertEquals(1, repository.count())
                    assertEquals(5000L, repository.loadAll().single().priceMinor)
                    assertEquals("Alex", repository.loadAll().single().updatedBy)

                    repository.upsert(record("minecraft:diamond", 7500L, by = "Sam"))
                    assertEquals(1, repository.count())
                    assertEquals(7500L, repository.loadAll().single().priceMinor)
                    assertEquals("Sam", repository.loadAll().single().updatedBy)
                }
            }
        }

    @Test
    fun `insertMissing never overwrites an existing price on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val repository = JdbcWorthRepository(runtime, "default")

                    repository.upsert(record("minecraft:dirt", 10L))
                    repository.insertMissing(
                        listOf(
                            record("minecraft:dirt", 999L),
                            record("minecraft:stone", 20L),
                        )
                    )

                    val byItem = repository.loadAll().associate { it.itemId to it.priceMinor }
                    assertEquals(10L, byItem["minecraft:dirt"])
                    assertEquals(20L, byItem["minecraft:stone"])
                    assertEquals(2, repository.count())
                }
            }
        }

    @Test
    fun `namespaces are isolated on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val first = JdbcWorthRepository(runtime, "first")
                    val second = JdbcWorthRepository(runtime, "second")

                    first.upsert(record("minecraft:diamond", 5000L))

                    assertEquals(1, first.count())
                    assertEquals(0, second.count())
                }
            }
        }

    private fun backends(): List<DatabaseType> =
        listOf(
            DatabaseType.SQLITE,
            DatabaseType.H2,
        )

}
