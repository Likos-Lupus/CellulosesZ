package top.likoslupus.cellulosesz.movement.spawn

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.nio.file.Files
import java.nio.file.Path

class FileSpawnRepositoryTest {

    @TempDir
    lateinit var directory: Path

    private fun repository(): FileSpawnRepository =
        FileSpawnRepository { directory }

    @Test
    fun `reads null when unset and round trips a position`() =
        runBlocking {
            assertNull(repository().read())

            val position = StoredPosition(
                dimension = "minecraft:overworld",
                x = 1.0,
                y = 2.0,
                z = 3.0,
                yaw = 10f,
                pitch = 20f
            )
            repository().write(position)

            assertEquals(position, repository().read())
        }

    @Test
    fun `writing null clears the spawn`() =
        runBlocking {
            val repo = repository()
            repo.write(
                StoredPosition(
                    dimension = "minecraft:overworld",
                    x = 1.0,
                    y = 2.0,
                    z = 3.0,
                    yaw = 0f,
                    pitch = 0f
                )
            )

            repo.write(null)

            assertNull(repo.read())
        }

    @Test
    fun `corrupt data is rejected and preserved`() {
        Files.writeString(directory.resolve("spawn.json"), "{ not json")

        assertThrows(SpawnDataException::class.java) {
            runBlocking { repository().read() }
        }
        assertEquals(
            "{ not json",
            Files.readString(directory.resolve("spawn.json"))
        )
    }

}
