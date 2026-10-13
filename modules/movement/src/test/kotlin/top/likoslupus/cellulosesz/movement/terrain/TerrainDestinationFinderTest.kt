package top.likoslupus.cellulosesz.movement.terrain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.TerrainSettings
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.safety.SurfaceKind
import kotlin.math.floor

class TerrainDestinationFinderTest {

    private val finder = TerrainDestinationFinder()
    private val settings = TerrainSettings(
        enabled = true,
        maxJumpDistance = 32,
        requireLoadedChunks = true,
        allowWater = false,
    )

    private fun origin(
        x: Double = 10.0,
        y: Double = 64.0,
        z: Double = 10.0,
    ) = StoredPosition("minecraft:overworld", x, y, z, 0f, 0f)

    @Test
    fun `surface lands on top of the highest block`() {
        val probe = FakeProbe(
            tops = mapOf(10 to 10 to 70),
            bottoms = mapOf(10 to 10 to 4),
            safe = setOf(cell(10, 71, 10)),
        )

        val found = assertInstanceOf(
            TerrainResult.Found::class.java,
            finder.surface(origin(), settings, probe),
        )
        assertEquals(71.0, found.position.y)
    }

    @Test
    fun `surface scans downward when the top standing space is blocked`() {
        val probe = FakeProbe(
            tops = mapOf(10 to 10 to 70),
            bottoms = mapOf(10 to 10 to 4),
            safe = setOf(cell(10, 69, 10)),
        )

        val found = assertInstanceOf(
            TerrainResult.Found::class.java,
            finder.surface(origin(), settings, probe),
        )
        assertEquals(69.0, found.position.y)
    }

    @Test
    fun `bottom lands on the first safe space above the lowest block`() {
        val probe = FakeProbe(
            tops = mapOf(10 to 10 to 70),
            bottoms = mapOf(10 to 10 to 5),
            safe = setOf(cell(10, 6, 10)),
        )

        val found = assertInstanceOf(
            TerrainResult.Found::class.java,
            finder.bottom(origin(), settings, probe),
        )
        assertEquals(6.0, found.position.y)
    }

    @Test
    fun `jump centres on the targeted block`() {
        val probe = FakeProbe(
            tops = mapOf(12 to 14 to 70),
            bottoms = mapOf(12 to 14 to 4),
            safe = setOf(cell(12, 71, 14)),
        )

        val found = assertInstanceOf(
            TerrainResult.Found::class.java,
            finder.jump(
                origin(),
                12,
                14,
                settings,
                probe
            ),
        )
        assertEquals(12.5, found.position.x)
        assertEquals(14.5, found.position.z)
        assertEquals(71.0, found.position.y)
    }

    @Test
    fun `unloaded column is reported`() {
        val probe = FakeProbe(loaded = false)
        assertInstanceOf(
            TerrainResult.ChunkNotLoaded::class.java,
            finder.surface(origin(), settings, probe),
        )
    }

    @Test
    fun `water is only accepted when allowed`() {
        val probe = FakeProbe(
            tops = mapOf(10 to 10 to 70),
            bottoms = mapOf(10 to 10 to 4),
            surface = SurfaceKind.WATER,
        )

        assertInstanceOf(
            TerrainResult.NoSafeSpot::class.java,
            finder.surface(origin(), settings, probe),
        )
        assertInstanceOf(
            TerrainResult.Found::class.java,
            finder.surface(
                origin(),
                settings.copy(allowWater = true),
                probe
            ),
        )
    }

    private fun cell(
        x: Int,
        y: Int,
        z: Int,
    ) =
        Triple(x, y, z)

    private class FakeProbe(
        private val tops: Map<Pair<Int, Int>, Int> = emptyMap(),
        private val bottoms: Map<Pair<Int, Int>, Int> = emptyMap(),
        private val safe: Set<Triple<Int, Int, Int>> = emptySet(),
        private val surface: SurfaceKind = SurfaceKind.BLOCKED,
        private val loaded: Boolean = true,
    ) : TerrainProbe {

        override fun isChunkLoaded(blockX: Int, blockZ: Int): Boolean =
            loaded

        override fun isWithinHeight(y: Double): Boolean =
            y in -64.0..320.0

        override fun highestBlockY(blockX: Int, blockZ: Int): Int? =
            tops[blockX to blockZ]

        override fun lowestBlockY(blockX: Int, blockZ: Int): Int? =
            bottoms[blockX to blockZ]

        override fun classify(
            x: Double,
            y: Double,
            z: Double,
        ): SurfaceKind {
            val cell = Triple(
                floor(x).toInt(),
                floor(y).toInt(),
                floor(z).toInt(),
            )
            return if (cell in safe) SurfaceKind.SAFE else surface
        }

    }

}
