package top.likoslupus.cellulosesz.movement.home

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class FileHomeRepositoryTest {

    @TempDir
    lateinit var directory: Path

    private fun repository(): FileHomeRepository =
        FileHomeRepository { directory }

    private fun position(): StoredPosition =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = 1.0,
            y = 2.0,
            z = 3.0,
            yaw = 0f,
            pitch = 0f
        )

    @Test
    fun `list is empty for an unknown owner`() =
        runBlocking {
            assertTrue(repository().list(UUID.randomUUID()).isEmpty())
        }

    @Test
    fun `put then list round trips`() =
        runBlocking {
            val owner = UUID.randomUUID()
            val name = HomeName.parse("base")!!

            repository().put(owner, Home(name, position()))

            val homes = repository().list(owner)
            assertEquals(listOf(name), homes.map { it.name })
            assertEquals(position(), homes.single().position)
        }

    @Test
    fun `put overwrites the same name`() =
        runBlocking {
            val owner = UUID.randomUUID()
            val name = HomeName.parse("base")!!
            val repo = repository()

            repo.put(owner, Home(name, position()))
            repo.put(owner, Home(name, position().copy(x = 99.0)))

            val homes = repo.list(owner)
            assertEquals(1, homes.size)
            assertEquals(99.0, homes.single().position.x)
        }

    @Test
    fun `remove reports whether a home existed`() =
        runBlocking {
            val owner = UUID.randomUUID()
            val name = HomeName.parse("base")!!
            val repo = repository()
            repo.put(owner, Home(name, position()))

            assertTrue(repo.remove(owner, name))
            assertFalse(repo.remove(owner, name))
            assertTrue(repo.list(owner).isEmpty())
        }

    @Test
    fun `corrupt data is not silently replaced`() {
        val owner = UUID.randomUUID()
        val file = directory.resolve("homes").resolve("$owner.json")
        Files.createDirectories(file.parent)
        Files.writeString(
            file,
            "{ this is not json"
        )

        assertThrows(HomeDataException::class.java) {
            runBlocking { repository().list(owner) }
        }
        assertTrue(Files.exists(file))
        assertEquals(
            "{ this is not json",
            Files.readString(file)
        )
    }

    @Test
    fun `unsupported schema is rejected`() {
        val owner = UUID.randomUUID()
        val file = directory.resolve("homes").resolve("$owner.json")
        Files.createDirectories(file.parent)
        Files.writeString(
            file,
            /* language=JSON */ """{ "schemaVersion": 99, "homes": {} }"""
        )

        assertThrows(HomeDataException::class.java) {
            runBlocking { repository().list(owner) }
        }
    }

}
