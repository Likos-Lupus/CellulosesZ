package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/** Deterministic nearest-safe search bounds; hard caps keep safety scans from becoming a hotspot. */
@Serializable
public data class TeleportSafetySettings(
    public val enabled: Boolean = true,
    public val searchHorizontalRadius: Int = 3,
    public val searchVerticalRadius: Int = 8,
    public val allowWater: Boolean = false,
)
