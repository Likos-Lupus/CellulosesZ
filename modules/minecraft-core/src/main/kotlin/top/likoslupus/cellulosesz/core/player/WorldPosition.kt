package top.likoslupus.cellulosesz.core.player

/**
 * An immutable world position with a resource-location dimension id. It carries no business meaning
 * and no live Minecraft object, so it is safe to persist and to pass across suspending boundaries.
 */
public data class WorldPosition(
    public val dimension: String,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val yaw: Float,
    public val pitch: Float,
) {

    init {
        require(dimension.isNotBlank()) { "dimension must not be blank" }
        require(x.isFinite() && y.isFinite() && z.isFinite()) { "coordinates must be finite" }
        require(yaw.isFinite() && pitch.isFinite()) { "rotation must be finite" }
    }

}
