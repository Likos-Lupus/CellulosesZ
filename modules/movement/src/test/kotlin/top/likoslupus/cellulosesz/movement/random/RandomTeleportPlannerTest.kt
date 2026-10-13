package top.likoslupus.cellulosesz.movement.random

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class RandomTeleportPlannerTest {

    private fun region(minimumRadius: Double, maximumRadius: Double) =
        RandomTeleportRegion(
            name = checkNotNull(RegionName.parse("wild")),
            dimension = "minecraft:overworld",
            centerX = 100.0,
            centerZ = -50.0,
            minimumRadius = minimumRadius,
            maximumRadius = maximumRadius,
            enabled = true,
            revision = 1L,
        )

    private fun distanceSquared(candidate: RandomCandidate): Double =
        (candidate.x - 100.0) * (candidate.x - 100.0) +
                (candidate.z - -50.0) * (candidate.z - -50.0)

    @Test
    fun `candidate stays inside the annulus`() {
        val planner = RandomTeleportPlanner(Random(42))
        repeat(1_000) {
            val candidate = planner.candidate(region(100.0, 500.0))
            val squared = distanceSquared(candidate)
            assertTrue(squared >= 100.0 * 100.0 - 1e-6)
            assertTrue(squared <= 500.0 * 500.0 + 1e-6)
            assertEquals("minecraft:overworld", candidate.dimension)
        }
    }

    @Test
    fun `candidate respects a zero minimum radius`() {
        val planner = RandomTeleportPlanner(Random(7))
        repeat(1_000) {
            val candidate = planner.candidate(region(0.0, 250.0))
            val squared = distanceSquared(candidate)
            assertTrue(squared <= 250.0 * 250.0 + 1e-6)
        }
    }

    @Test
    fun `an annulus far outside a rectangle does not intersect it`() {
        assertFalse(
            intersectsRectangle(
                region(10.0, 20.0),
                minX = -100.0,
                maxX = -90.0,
                minZ = -100.0,
                maxZ = -90.0,
            )
        )
    }

    @Test
    fun `a rectangle wholly inside the hole does not intersect the annulus`() {
        assertFalse(
            intersectsRectangle(
                region(10.0, 20.0),
                minX = -5.0,
                maxX = 5.0,
                minZ = -5.0,
                maxZ = 5.0,
            )
        )
    }

    @Test
    fun `a rectangle crossing the annulus intersects it`() {
        assertTrue(
            intersectsRectangle(
                region(10.0, 20.0),
                minX = 110.0,
                maxX = 125.0,
                minZ = -55.0,
                maxZ = -45.0,
            )
        )
    }

    @Test
    fun `a rectangle centered on the region intersects a zero minimum annulus`() {
        assertTrue(
            intersectsRectangle(
                region(0.0, 100.0),
                minX = 95.0,
                maxX = 105.0,
                minZ = -55.0,
                maxZ = -45.0,
            )
        )
    }

}
