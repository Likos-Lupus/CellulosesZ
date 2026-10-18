package top.likoslupus.cellulosesz.movement.world

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorldDirectoryTest {

    private val available = setOf(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end",
    )

    private val directory = WorldDirectory(
        mapOf(
            "overworld" to "minecraft:overworld",
            "nether" to "minecraft:the_nether",
        )
    )

    @Test
    fun `an alias resolves to the dimension it names`() {
        assertEquals(
            "minecraft:the_nether",
            directory.resolve("nether", available),
        )
    }

    @Test
    fun `a raw dimension id always resolves`() {
        assertEquals(
            "minecraft:the_end",
            directory.resolve("minecraft:the_end", available),
        )
    }

    @Test
    fun `alias resolution is case insensitive`() {
        assertEquals(
            "minecraft:the_nether",
            directory.resolve("NETHER", available),
        )
    }

    @Test
    fun `an unmatched name does not resolve`() {
        assertNull(directory.resolve("moon", available))
    }

    @Test
    fun `an alias whose dimension is not loaded does not resolve`() {
        val unloaded = WorldDirectory(mapOf("moon" to "example:moon"))
        assertNull(unloaded.resolve("moon", available))
    }

    @Test
    fun `the canonical name prefers a configured alias`() {
        assertEquals(
            "nether",
            directory.aliasOf("minecraft:the_nether")
        )
        assertNull(directory.aliasOf("minecraft:the_end"))
    }

    @Test
    fun `entries are sorted by dimension and carry their alias`() {
        val entries = directory.entries(available)
        assertEquals(
            listOf(
                "minecraft:overworld",
                "minecraft:the_end",
                "minecraft:the_nether",
            ),
            entries.map { it.dimension },
        )
        assertEquals("overworld", entries.first().alias)
        assertNull(entries[1].alias)
        assertEquals("nether", entries.last().alias)
    }

}
