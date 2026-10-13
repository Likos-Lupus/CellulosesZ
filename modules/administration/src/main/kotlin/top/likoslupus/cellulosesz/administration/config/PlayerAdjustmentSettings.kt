package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/** Bounds for operator player adjustments: `/exp` and `/speed`. */
@Serializable
public data class PlayerAdjustmentSettings(
    public val maximumExperienceAdjustment: Int = 1_000_000,
    public val minimumSpeedMultiplier: Double = 0.1,
    public val maximumSpeedMultiplier: Double = 3.0,
)
