package top.likoslupus.cellulosesz.movement.teleport.history

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class FileTeleportHistoryRepositoryTest {

    @TempDir
    lateinit var directory: Path

    private val repository = FileTeleportHistoryRepository { directory }
    private val player = UUID.randomUUID()

    private fun position(y: Double = 64.0) =
        StoredPosition(
            dimension = "minecraft:overworld",
            x = 1.0,
            y = y,
            z = 2.0,
            yaw = 0f,
            pitch = 0f
        )

    @Test
    fun `missing history reads as null`() = runBlocking {
        assertNull(repository.read(player))
    }

    @Test
    fun `round trips a position`() = runBlocking {
        repository.write(player, position())

        assertEquals(position(), repository.read(player))
    }

    @Test
    fun `corrupt history raises a typed error`() {
        val file = directory
                .resolve("teleport-history")
                .resolve("$player.json")
        Files.createDirectories(file.parent)
        Files.writeString(file, "{ not json")

        assertThrows(TeleportHistoryDataException::class.java) {
            runBlocking { repository.read(player) }
        }
    }

    @Test
    fun `schema mismatch raises a typed error`() {
        val file = directory.resolve("teleport-history").resolve("$player.json")
        Files.createDirectories(file.parent)
        Files.writeString(
            file,
            /* language=JSON */ """{ "schemaVersion": 99, "previous": null }"""
        )

        assertThrows(TeleportHistoryDataException::class.java) {
            runBlocking { repository.read(player) }
        }
    }

}
