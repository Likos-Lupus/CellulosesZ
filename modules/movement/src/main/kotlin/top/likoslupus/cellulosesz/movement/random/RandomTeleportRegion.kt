package top.likoslupus.cellulosesz.movement.random

@JvmInline
internal value class RegionName private constructor(val value: String) {

    companion object {

        val DEFAULT: RegionName = RegionName("default")

        private val PATTERN = Regex("[a-z0-9_-]{1,32}")

        fun parse(raw: String): RegionName? =
            if (PATTERN.matches(raw)) {
                RegionName(raw)
            } else {
                null
            }

    }

}

/**
 * A named search area. The center is a point; random candidates are drawn from the annulus between
 * [minimumRadius] and [maximumRadius] around it, uniformly by area. [revision] increments whenever the
 * center or the radii change, so a running search can be cancelled if the region moved underneath it.
 */
internal data class RandomTeleportRegion(
    val name: RegionName,
    val dimension: String,
    val centerX: Double,
    val centerZ: Double,
    val minimumRadius: Double,
    val maximumRadius: Double,
    val enabled: Boolean,
    val revision: Long,
)
