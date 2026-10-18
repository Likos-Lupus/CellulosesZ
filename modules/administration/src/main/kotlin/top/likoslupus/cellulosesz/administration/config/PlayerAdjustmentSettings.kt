package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/**
 * Bounds and policy for operator player adjustments: `/exp`, `/speed`, `/heal`, `/fly` and `/god`.
 * Every duration is in seconds; `0` disables the respective cooldown.
 */
@Serializable
public data class PlayerAdjustmentSettings(
    public val maximumExperienceAdjustment: Int = 1_000_000,
    public val minimumSpeedMultiplier: Double = 0.1,
    public val maximumSpeedMultiplier: Double = 3.0,
    /** Per-operator cooldown for `/heal`; bypassed by `HEAL_COOLDOWN_BYPASS`. `0` disables it. */
    public val healCooldownSeconds: Long = 0,
    /** Remove all mob effects from the healed player. */
    public val removeEffectsOnHeal: Boolean = false,
    /** Dimension ids where `/god` may not be enabled and is stripped on entry. */
    public val noGodWorlds: List<String> = emptyList(),
    /** Strip invulnerability from a god player when they disconnect. */
    public val removeGodOnDisconnect: Boolean = true,
    /** Grant flight on join to players allowed to fly (`cellulosesz.command.fly`). */
    public val flyOnJoin: Boolean = false,
    /** Disable flight when a player changes dimension (creative/spectator are never affected). */
    public val resetFlyOnWorldChange: Boolean = false,
    /** Keep the operator-set flight state across a game-mode change. */
    public val preserveFlyOnGamemodeChange: Boolean = true,
)
