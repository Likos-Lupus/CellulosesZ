package top.likoslupus.cellulosesz.movement

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import top.likoslupus.cellulosesz.movement.home.Home
import top.likoslupus.cellulosesz.movement.home.HomeName
import top.likoslupus.cellulosesz.movement.home.HomeRenameOutcome
import top.likoslupus.cellulosesz.movement.home.JdbcHomeRepository
import top.likoslupus.cellulosesz.movement.request.JdbcTpAutoRepository
import top.likoslupus.cellulosesz.movement.request.TpAutoMode
import top.likoslupus.cellulosesz.movement.spawn.JdbcSpawnRepository
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.history.JdbcTeleportHistoryRepository
import top.likoslupus.cellulosesz.movement.warp.JdbcWarpRepository
import top.likoslupus.cellulosesz.movement.warp.Warp
import top.likoslupus.cellulosesz.movement.warp.WarpMetadata
import top.likoslupus.cellulosesz.movement.warp.WarpName
import java.nio.file.Path
import java.time.Instant
import java.util.*

class MovementPersistenceTest {

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
            listOf(MovementSqlSchema)
        )
        return runtime
    }

    private fun position(x: Double) =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = x, y = 64.0, z = 0.0,
            yaw = 0f, pitch = 0f
        )

    @Test
    fun `homes round-trip and are namespaced`() =
        runBlocking {
            runtime().use { runtime ->
                val repository = JdbcHomeRepository(runtime, "default")
                val owner = UUID.randomUUID()
                val name = checkNotNull(HomeName.parse("base"))

                repository.put(owner, Home(name, position(1.0)))
                repository.put(owner, Home(name, position(2.0)))

                val homes = repository.list(owner)
                assertEquals(1, homes.size)
                assertEquals(2.0, homes.single().position.x)
                assertTrue(
                    JdbcHomeRepository(
                        runtime,
                        "other"
                    ).list(owner).isEmpty()
                )
                assertTrue(repository.remove(owner, name))
                assertTrue(repository.list(owner).isEmpty())
            }
        }

    @Test
    fun `warps round-trip with their sidecar metadata`() =
        runBlocking {
            runtime().use {
                val repository = JdbcWarpRepository(it, "default")
                val name = checkNotNull(WarpName.parse("hub"))
                val metadata = WarpMetadata(
                    "Alice",
                    Instant.ofEpochMilli(1_000L),
                    Instant.ofEpochMilli(2_000L),
                )
                repository.put(Warp(name, position(5.0)), metadata)
                assertEquals(1, repository.list().size)
                assertEquals(5.0, repository.get(name)?.position?.x)
                assertEquals("Alice", repository.metadata(name)?.createdBy)
                assertEquals(
                    Instant.ofEpochMilli(1_000L),
                    repository.metadata(name)?.createdAt
                )
                assertEquals(
                    Instant.ofEpochMilli(2_000L),
                    repository.metadata(name)?.updatedAt
                )
                assertTrue(repository.remove(name))
                assertNull(repository.get(name))
                assertNull(repository.metadata(name))
            }
        }

    @Test
    fun `home rename is atomic and refuses to overwrite`() =
        runBlocking {
            runtime().use { runtime ->
                val repository = JdbcHomeRepository(runtime, "default")
                val owner = UUID.randomUUID()
                val from = checkNotNull(HomeName.parse("old"))
                val to = checkNotNull(HomeName.parse("new"))

                repository.put(owner, Home(from, position(1.0)))

                assertEquals(
                    HomeRenameOutcome.Renamed,
                    repository.rename(owner, from, to)
                )
                val homes = repository.list(owner)
                assertEquals(1, homes.size)
                assertEquals(to, homes.single().name)
                assertEquals(1.0, homes.single().position.x)

                assertEquals(
                    HomeRenameOutcome.SourceMissing,
                    repository.rename(
                        owner,
                        from,
                        checkNotNull(HomeName.parse("other"))
                    ),
                )

                repository.put(
                    owner,
                    Home(
                        checkNotNull(HomeName.parse("keep")),
                        position(3.0)
                    )
                )
                assertEquals(
                    HomeRenameOutcome.DestinationExists,
                    repository.rename(
                        owner,
                        to,
                        checkNotNull(HomeName.parse("keep"))
                    ),
                )
                assertEquals(2, repository.list(owner).size)
            }
        }

    @Test
    fun `spawn can be set and cleared`() =
        runBlocking {
            runtime().use {
                val repository = JdbcSpawnRepository(it, "default")
                assertNull(repository.read())
                repository.write(position(9.0))
                assertEquals(9.0, repository.read()?.x)
                repository.write(null)
                assertNull(repository.read())
            }
        }

    @Test
    fun `tpauto preferences and allow-list round-trip`() =
        runBlocking {
            runtime().use { runtime ->
                val repository = JdbcTpAutoRepository(runtime, "default")
                val owner = UUID.randomUUID()
                val allowed = UUID.randomUUID()

                assertTrue(repository.loadAllBlocking().modes.isEmpty())

                repository.setMode(owner, TpAutoMode.ALL)
                repository.addAllowed(owner, allowed)

                val snapshot = repository.loadAllBlocking()
                assertEquals(TpAutoMode.ALL, snapshot.modes[owner])
                assertEquals(setOf(allowed), snapshot.allowed[owner])

                repository.removeAllowed(owner, allowed)
                repository.setMode(owner, TpAutoMode.OFF)
                val cleared = repository.loadAllBlocking()
                assertNull(cleared.modes[owner])
                assertNull(cleared.allowed[owner])

                assertTrue(
                    JdbcTpAutoRepository(
                        runtime,
                        "other"
                    ).loadAllBlocking().modes.isEmpty()
                )
            }
        }

    @Test
    fun `teleport history keeps one previous position per player`() =
        runBlocking {
            runtime().use {
                val repository = JdbcTeleportHistoryRepository(it, "default")
                val player = UUID.randomUUID()
                repository.write(player, position(3.0))
                repository.write(player, position(4.0))
                assertEquals(4.0, repository.read(player)?.x)
                assertNull(repository.read(UUID.randomUUID()))
            }
        }

}
