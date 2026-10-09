package top.likoslupus.cellulosesz.core.text.i18n.preference

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.util.*

class JdbcPlayerLanguagePreferenceRepositoryTest {

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
            listOf(LocalizationSqlSchema)
        )
        return runtime
    }

    @Test
    fun `preferences round-trip and are namespaced`() =
        runBlocking {
            runtime().use { runtime ->
                val repository = JdbcPlayerLanguagePreferenceRepository(
                    runtime,
                    "default"
                )
                val player = UUID.randomUUID()

                assertNull(repository.loadAll()[player])

                repository.set(player, LanguageId.ZH_CN)
                assertEquals(
                    LanguageId.ZH_CN,
                    repository.loadAll()[player]
                )

                repository.set(player, LanguageId.EN_US)
                assertEquals(
                    LanguageId.EN_US,
                    repository.loadAll()[player]
                )

                assertTrue(
                    JdbcPlayerLanguagePreferenceRepository(
                        runtime,
                        "other"
                    ).loadAll().isEmpty()
                )

                repository.remove(player)
                assertNull(repository.loadAll()[player])
            }
        }

}
