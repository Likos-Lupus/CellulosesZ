package top.likoslupus.cellulosesz.administration.jail

import java.time.Instant

/** An immutable jail location: where a jailed player is held. */
internal data class JailLocation(
    val name: JailName,
    val dimension: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val radius: Double,
    val createdAt: Instant,
    val createdBy: String?,
) {

    init {
        require(x.isFinite() && y.isFinite() && z.isFinite()) {
            "jail coordinates must be finite"
        }
        require(yaw.isFinite() && pitch.isFinite()) {
            "jail rotation must be finite"
        }
        require(radius.isFinite() && radius >= 0.0) {
            "jail radius must be finite and >= 0"
        }
    }

}
