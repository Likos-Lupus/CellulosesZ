package top.likoslupus.cellulosesz.movement.teleport

import kotlinx.serialization.Serializable

/**
 * Storage model for a position. It is intentionally decoupled from Minecraft: the dimension stays a
 * stable resource-location string so a data file still reads even when the dimension mod that wrote
 * it is unloaded. Runtime resolution to a `ServerLevel` happens in the teleport backend.
 */
@Serializable
internal data class StoredPosition(
    val dimension: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
) {

    init {
        require(x.isFinite() && y.isFinite() && z.isFinite()) {
            "position coordinates must be finite"
        }
        require(yaw.isFinite() && pitch.isFinite()) {
            "position rotation must be finite"
        }
    }

}
