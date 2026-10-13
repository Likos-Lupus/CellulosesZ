package top.likoslupus.cellulosesz.movement.random

import java.util.*
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** A random horizontal sample inside a region, still to be resolved to a safe standing position. */
internal data class RandomCandidate(
    val dimension: String,
    val x: Double,
    val z: Double,
)

/**
 * Picks one candidate inside a region's annulus. Kept pure and injectable so the area-uniform bounds
 * behaviour is unit-testable without a world: `radius = sqrt(min^2 + rand * (max^2 - min^2))` spreads
 * candidates evenly by area, never clustered near the center.
 */
internal class RandomTeleportPlanner(
    private val random: Random = Random(),
) {

    fun candidate(region: RandomTeleportRegion): RandomCandidate {
        val minimumSquared = region.minimumRadius * region.minimumRadius
        val maximumSquared = region.maximumRadius * region.maximumRadius
        val distance = sqrt(minimumSquared + random.nextDouble() * (maximumSquared - minimumSquared))
        val angle = random.nextDouble(0.0, TWO_PI)
        return RandomCandidate(
            dimension = region.dimension,
            x = region.centerX + cos(angle) * distance,
            z = region.centerZ + sin(angle) * distance,
        )
    }

    private companion object {

        const val TWO_PI: Double = Math.PI * 2.0

    }

}

/**
 * Whether the annulus of [region] overlaps the closed rectangle `[minX, maxX] x [minZ, maxZ]`. The
 * distance from a point to a convex rectangle spans every value between the nearest and the farthest
 * corner, so a single interval overlap test is exact. Used to fail a search immediately when the world
 * border leaves no reachable area instead of burning every attempt.
 */
internal fun intersectsRectangle(
    region: RandomTeleportRegion,
    minX: Double,
    maxX: Double,
    minZ: Double,
    maxZ: Double,
): Boolean {
    val nearestX = region.centerX.coerceIn(minX, maxX)
    val nearestZ = region.centerZ.coerceIn(minZ, maxZ)
    val nearest = hypot(region.centerX - nearestX, region.centerZ - nearestZ)
    var farthest = 0.0
    for (cornerX in doubleArrayOf(minX, maxX)) {
        for (cornerZ in doubleArrayOf(minZ, maxZ)) {
            farthest = maxOf(farthest, hypot(region.centerX - cornerX, region.centerZ - cornerZ))
        }
    }
    return farthest >= region.minimumRadius && nearest <= region.maximumRadius
}
