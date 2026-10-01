package top.likoslupus.cellulosesz.movement.teleport

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredPosition(
    val dimension: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
)
