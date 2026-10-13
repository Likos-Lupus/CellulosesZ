package top.likoslupus.cellulosesz.player.nearby

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class NearbyFilterTest {

    private val originId = UUID.randomUUID()

    @Test
    fun `only same-dimension non-self players inside the radius are returned`() {
        val near = candidate(
            "Near",
            "minecraft:overworld",
            10.0,
            0.0
        )
        val far = candidate(
            "Far",
            "minecraft:overworld",
            500.0,
            0.0
        )
        val otherWorld = candidate(
            "Nether",
            "minecraft:the_nether",
            5.0,
            0.0
        )
        val self = candidate(
            "Self",
            "minecraft:overworld",
            0.0,
            0.0,
            id = originId
        )

        val selected = NearbyFilter.select(
            candidates = listOf(near, far, otherWorld, self),
            originId = originId,
            dimension = "minecraft:overworld",
            originX = 0.0,
            originZ = 0.0,
            radius = 100.0,
            maximumResults = 20,
            hideSpectators = true,
        )

        assertEquals(listOf("Near"), selected.map { it.name })
    }

    @Test
    fun `results are sorted by distance and capped`() {
        val candidates = listOf(
            candidate("B", "minecraft:overworld", 30.0, 0.0),
            candidate("A", "minecraft:overworld", 5.0, 0.0),
            candidate("C", "minecraft:overworld", 15.0, 0.0),
        )

        val selected = NearbyFilter.select(
            candidates = candidates,
            originId = originId,
            dimension = "minecraft:overworld",
            originX = 0.0,
            originZ = 0.0,
            radius = 100.0,
            maximumResults = 2,
            hideSpectators = true,
        )

        assertEquals(listOf("A", "C"), selected.map { it.name })
    }

    @Test
    fun `spectators are hidden only when configured`() {
        val spectator = candidate(
            "Spec",
            "minecraft:overworld",
            1.0,
            0.0,
            spectator = true
        )

        val hidden = NearbyFilter.select(
            candidates = listOf(spectator),
            originId = originId,
            dimension = "minecraft:overworld",
            originX = 0.0,
            originZ = 0.0,
            radius = 100.0,
            maximumResults = 20,
            hideSpectators = true,
        )
        val shown = NearbyFilter.select(
            candidates = listOf(spectator),
            originId = originId,
            dimension = "minecraft:overworld",
            originX = 0.0,
            originZ = 0.0,
            radius = 100.0,
            maximumResults = 20,
            hideSpectators = false,
        )

        assertEquals(emptyList<String>(), hidden.map { it.name })
        assertEquals(listOf("Spec"), shown.map { it.name })
    }

    private fun candidate(
        name: String,
        dimension: String,
        x: Double,
        z: Double,
        spectator: Boolean = false,
        id: UUID = UUID.randomUUID(),
    ): NearbyCandidate =
        NearbyCandidate(
            id = id,
            name = name,
            dimension = dimension,
            x = x,
            z = z,
            spectator = spectator,
        )

}
