package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/**
 * Bounds and pacing for `/tpr`. A random teleport searches a named region for a safe spot; every
 * limit here exists so a search stays bounded in time and in how much work it can pile up.
 */
@Serializable
public data class RandomTeleportSettings(
    public val enabled: Boolean = true,
    public val maxAttempts: Int = 16,
    public val maxRadius: Int = 10000,
    public val maxRegions: Int = 32,
    public val searchTimeoutSeconds: Long = 20,
    public val cooldownSeconds: Long = 120,
    public val maxConcurrentServerSearches: Int = 2,
    public val maxConcurrentPerPlayer: Int = 1,
    public val allowChunkGeneration: Boolean = false,
    public val allowWater: Boolean = false,
)
