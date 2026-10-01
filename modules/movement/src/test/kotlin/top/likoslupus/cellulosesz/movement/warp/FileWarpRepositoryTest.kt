package top.likoslupus.cellulosesz.movement.warp

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.nio.file.Files
import java.nio.file.Path

class FileWarpRepositoryTest {

    @TempDir
    lateinit var directory: Path

    private fun repository(): FileWarpRepository = FileWarpRepository { directory }

    @Test
    fun `warps survive a fresh repository instance`() =
        runBlocking {
            val name = WarpName.parse("hub")!!
            val position = StoredPosition("minecraft:overworld", 0.5, 65.0, 0.5, 0f, 0f)

            repository().put(Warp(name, position))

            val reopened = repository().get(name)
            assertEquals(position, reopened?.position)
        }

    @Test
    fun `list is sorted and removal reports existence`() =
        runBlocking {
            val repo = repository()
            repo.put(
                Warp(
                    WarpName.parse("zulu")!!,
                    StoredPosition(
                        "minecraft:overworld",
                        0.0,
                        0.0,
                        0.0,
                        0f,
                        0f
                    )
                )
            )
            repo.put(
                Warp(
                    WarpName.parse("alpha")!!,
                    StoredPosition(
                        "minecraft:overworld",
                        1.0,
                        0.0,
                        0.0,
                        0f,
                        0f
                    )
                )
            )

            assertEquals(listOf("alpha", "zulu"), repo.list().map { it.name.value })
            assertTrue(repo.remove(WarpName.parse("alpha")!!))
            assertFalse(repo.remove(WarpName.parse("alpha")!!))
        }

    @Test
    fun `corrupt data is rejected and preserved`() {
        Files.writeString(
            directory.resolve("warps.json"),
            "{ not json"
        )

        assertThrows(WarpDataException::class.java) {
            runBlocking { repository().list() }
        }
        assertEquals(
            "{ not json",
            Files.readString(directory.resolve("warps.json"))
        )
    }

}
