package top.likoslupus.cellulosesz.api.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WorldContractsTest {

    @Test
    fun worldResolution_resolved() {
        val resolution = WorldResolution.resolved("minecraft:overworld")
        assertEquals(
            resolution,
            WorldResolution.Resolved("minecraft:overworld")
        )
    }

    @Test
    fun worldResolution_notFound() {
        assertEquals(WorldResolution.NotFound, WorldResolution.notFound())
    }

    @Test
    fun worldResolution_ambiguous() {
        val resolution = WorldResolution.ambiguous(listOf("world_1", "world_2"))
        assertEquals(
            resolution,
            WorldResolution.Ambiguous(listOf("world_1", "world_2"))
        )
    }

    @Test
    fun worldResolution_ambiguous_requiresCandidates() {
        assertThrows(IllegalArgumentException::class.java) {
            WorldResolution.Ambiguous(listOf("only"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorldResolution.Ambiguous(emptyList())
        }
    }

    @Test
    fun worldDirectory_defaultResolveLoadedWorld() {
        val directory = object : WorldDirectory {
            override fun loadedWorldIds(): List<String> = listOf("minecraft:overworld")

            override fun resolve(input: String): WorldResolution =
                if (input == "overworld") {
                    WorldResolution.resolved("minecraft:overworld")
                } else {
                    WorldResolution.notFound()
                }
        }

        assertEquals(
            "minecraft:overworld",
            directory.resolveLoadedWorld("overworld")
        )
        assertEquals(
            null,
            directory.resolveLoadedWorld("missing")
        )
    }

}
