package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

@Serializable
public data class TeleportRequestSettings(
    public val timeoutSeconds: Long = 60,
    public val maxIncomingPerPlayer: Int = 5,
)
