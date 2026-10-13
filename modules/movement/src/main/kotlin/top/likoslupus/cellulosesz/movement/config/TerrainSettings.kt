package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/** Rules for `/top`, `/bottom` and `/jump`: how far a jump may reach and which spots count as safe. */
@Serializable
public data class TerrainSettings(
    public val enabled: Boolean = true,
    public val maxJumpDistance: Int = 32,
    public val requireLoadedChunks: Boolean = true,
    public val allowWater: Boolean = false,
)
