package top.likoslupus.cellulosesz.movement.compass

import kotlin.math.roundToInt

/** The eight compass points, ordered clockwise from north. */
internal enum class CompassPoint {

    NORTH,
    NORTH_EAST,
    EAST,
    SOUTH_EAST,
    SOUTH,
    SOUTH_WEST,
    WEST,
    NORTH_WEST,

}

/**
 * A compass reading derived from a Minecraft yaw. Minecraft's yaw is 0 facing south (+Z) increasing
 * clockwise, while a compass bearing is 0 facing north increasing clockwise, so the conversion is
 * `(yaw + 180) mod 360`.
 */
internal data class CompassReading(
    val point: CompassPoint,
    val bearingDegrees: Int,
)

internal object CompassBearing {

    private val POINTS: List<CompassPoint> = CompassPoint.entries

    fun read(yaw: Float): CompassReading {
        val bearing = normalize(yaw.toDouble() + 180.0)
        val index = (((bearing + 22.5) / 45.0).toInt()) % POINTS.size
        return CompassReading(
            point = POINTS[index],
            bearingDegrees = bearing.roundToInt() % 360,
        )
    }

    private fun normalize(degrees: Double): Double {
        val wrapped = degrees % 360.0
        return if (wrapped < 0.0) wrapped + 360.0 else wrapped
    }

}
