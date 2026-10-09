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
import top.likoslupus.cellulosesz.movement.home.JdbcHomeRepository
import top.likoslupus.cellulosesz.movement.spawn.JdbcSpawnRepository
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.history.JdbcTeleportHistoryRepository
import top.likoslupus.cellulosesz.movement.warp.JdbcWarpRepository
import top.likoslupus.cellulosesz.movement.warp.Warp
import top.likoslupus.cellulosesz.movement.warp.WarpName
import java.nio.file.Path
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
    fun `warps round-trip`() =
        runBlocking {
            runtime().use {
                val repository = JdbcWarpRepository(it, "default")
                val name = checkNotNull(WarpName.parse("hub"))
                repository.put(Warp(name, position(5.0)))
                assertEquals(1, repository.list().size)
                assertEquals(5.0, repository.get(name)?.position?.x)
                assertTrue(repository.remove(name))
                assertNull(repository.get(name))
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
