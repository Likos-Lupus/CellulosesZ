package top.likoslupus.cellulosesz.core.preference

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.util.*
import kotlinx.coroutines.runBlocking

/**
 * The shared preference table exercised against both supported local backends (SQLite and H2). The
 * row is one key/value pair per player and preference, and a corrupt key must be reported rather
 * than silently dropped.
 */
class JdbcPlayerPreferenceRepositoryTest {

    @TempDir
    lateinit var directory: Path

    private fun runtime(type: DatabaseType): HikariDatabaseRuntime {
        val descriptor = when (type) {
            DatabaseType.H2 -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    type = DatabaseType.H2,
                    h2 = H2Settings(path = directory.resolve("cz_pref_h2").toString()),
                ),
                directory,
            )

            else -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    sqlite = SqliteSettings(
                        path = directory.resolve("cz_pref_sqlite.db").toString()
                    )
                ),
                directory,
            )
        }
        val runtime = HikariDatabaseRuntime.create(descriptor)
        SchemaInitializer.initializeBlocking(runtime, listOf(PreferenceSqlSchema))
        return runtime
    }

    private fun backends(): List<DatabaseType> =
        listOf(
            DatabaseType.SQLITE,
            DatabaseType.H2,
        )

    @Test
    fun `a stored value survives a fresh load on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val repository = JdbcPlayerPreferenceRepository(runtime, "default")
                    val alice = UUID.randomUUID()

                    repository.save(alice, PlayerPreference.TELEPORT_ENABLED, false)

                    val reloaded = JdbcPlayerPreferenceRepository(runtime, "default")
                            .loadAllBlocking()
                    assertEquals(
                        mapOf(PlayerPreference.TELEPORT_ENABLED to false),
                        reloaded[alice],
                    )
                }
            }
        }

    @Test
    fun `saving the same key again replaces the previous value on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val repository = JdbcPlayerPreferenceRepository(runtime, "default")
                    val alice = UUID.randomUUID()

                    repository.save(alice, PlayerPreference.ACCEPTING_PAY, false)
                    repository.save(alice, PlayerPreference.ACCEPTING_PAY, true)

                    val stored = repository.loadAllBlocking().getValue(alice)
                    assertEquals(1, stored.size)
                    assertEquals(true, stored[PlayerPreference.ACCEPTING_PAY])
                }
            }
        }

    @Test
    fun `rows are scoped by namespace on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val alice = UUID.randomUUID()
                    JdbcPlayerPreferenceRepository(runtime, "one")
                            .save(alice, PlayerPreference.PAY_CONFIRMATION, true)

                    assertTrue(
                        JdbcPlayerPreferenceRepository(runtime, "two")
                                .loadAllBlocking()
                                .isEmpty()
                    )
                    assertEquals(
                        true,
                        JdbcPlayerPreferenceRepository(runtime, "one")
                                .loadAllBlocking()
                                .getValue(alice)
                                .getValue(PlayerPreference.PAY_CONFIRMATION),
                    )
                }
            }
        }

    @Test
    fun `the service reads defaults then publishes only persisted writes on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val repository = JdbcPlayerPreferenceRepository(runtime, "default")
                    val alice = UUID.randomUUID()
                    val service = PlayerPreferenceService(repository) {
                        mapOf(PlayerPreference.TELEPORT_ENABLED to true)
                    }
                    service.hydrate(repository.loadAllBlocking())

                    assertTrue(service.isEnabled(alice, PlayerPreference.TELEPORT_ENABLED))

                    assertEquals(
                        PreferenceWriteResult.Updated(false),
                        service.set(alice, PlayerPreference.TELEPORT_ENABLED, false),
                    )
                    assertFalse(service.isEnabled(alice, PlayerPreference.TELEPORT_ENABLED))
                    assertEquals(
                        PreferenceWriteResult.Unchanged(false),
                        service.set(alice, PlayerPreference.TELEPORT_ENABLED, false),
                    )

                    val reloaded = PlayerPreferenceService(repository) {
                        mapOf(PlayerPreference.TELEPORT_ENABLED to true)
                    }
                    reloaded.hydrate(repository.loadAllBlocking())
                    assertFalse(reloaded.isEnabled(alice, PlayerPreference.TELEPORT_ENABLED))
                }
            }
        }

    @Test
    fun `an unknown stored key is reported as corrupt on every backend`() =
        backends().forEach { type ->
            runtime(type).use { runtime ->
                val alice = UUID.randomUUID()
                runtime.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${PreferenceSqlSchema.PREFERENCES} (
                            namespace,
                            player_uuid,
                            preference_key,
                            preference_value
                        )
                        VALUES (?, ?, ?, ?);
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, "default")
                        statement.setString(2, alice.toString())
                        statement.setString(3, "not_a_real_preference")
                        statement.setBoolean(4, true)
                        statement.executeUpdate()
                    }
                    Unit
                }

                assertThrows(PlayerPreferenceDataException::class.java) {
                    JdbcPlayerPreferenceRepository(runtime, "default").loadAllBlocking()
                }
            }
        }

}
