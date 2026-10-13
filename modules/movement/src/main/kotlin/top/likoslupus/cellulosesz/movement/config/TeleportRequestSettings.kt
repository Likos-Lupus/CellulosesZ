package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

@Serializable
public data class TeleportRequestSettings(
    public val timeoutSeconds: Long = 60,
    public val maxIncomingPerPlayer: Int = 5,
    public val maxOutgoingPerPlayer: Int = 16,
    public val maxRequestsServerWide: Int = 4096,
    public val maxBatchRecipients: Int = 16,
    public val batchCooldownSeconds: Long = 120,
    public val maxAutoAllowedPlayers: Int = 128,
)
