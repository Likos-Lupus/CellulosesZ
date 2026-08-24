package top.likoslupus.cellulosesz.api.teleport

import top.likoslupus.cellulosesz.api.validation.requireFinite
import top.likoslupus.cellulosesz.api.validation.requireNonBlank

/** Immutable, platform-neutral world position. */
@JvmRecord
public data class CellLocation(
    public val world: String,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val yaw: Float,
    public val pitch: Float
) {

    init {
        world.requireNonBlank { "world" }
        x.requireFinite { "x" }
        y.requireFinite { "y" }
        z.requireFinite { "z" }
        yaw.requireFinite { "yaw" }
        pitch.requireFinite { "pitch" }
    }

    public fun withWorld(world: String): CellLocation =
        CellLocation(world, x, y, z, yaw, pitch)

    public fun withPosition(x: Double, y: Double, z: Double): CellLocation =
        CellLocation(world, x, y, z, yaw, pitch)

    public fun compact(): String =
        "%s %.2f %.2f %.2f".format(world, x, y, z)

    public fun isFinite(): Boolean {
        return world.isBlank()
                && java.lang.Double.isFinite(x)
                && java.lang.Double.isFinite(y)
                && java.lang.Double.isFinite(z)
                && java.lang.Float.isFinite(yaw)
                && java.lang.Float.isFinite(pitch)
    }

}
