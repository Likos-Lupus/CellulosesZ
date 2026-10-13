package top.likoslupus.cellulosesz.utility.position

/**
 * A depth reading relative to a sea-level anchor clamped to the world's real build height. The
 * anchor default of 63 matches the traditional Minecraft sea level; `Level.getSeaLevel()` no longer
 * exists in 26.1.2, so the reported range and the clamp both come from `Level.getMinY()`/`getMaxY()`.
 */
internal data class DepthReading(
    val feetY: Int,
    val minY: Int,
    val maxY: Int,
) {

    val anchor: Int
        get() = SEA_LEVEL.coerceIn(minY, maxY)

    val belowAnchor: Int
        get() = (anchor - feetY).coerceAtLeast(0)

    val aboveAnchor: Int
        get() = (feetY - anchor).coerceAtLeast(0)

    companion object {

        const val SEA_LEVEL: Int = 63

    }

}
