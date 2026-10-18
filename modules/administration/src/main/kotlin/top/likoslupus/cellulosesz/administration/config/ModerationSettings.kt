package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/**
 * Runtime policy for the moderation capability. All durations are in seconds; `null` means
 * unlimited. Feature code never reads this directly from disk; the application supplies a
 * snapshot function.
 */
@Serializable
public data class ModerationSettings(
    public val enabled: Boolean = true,
    public val protectOperators: Boolean = true,
    public val defaultKickReason: String = "Kicked by an administrator",
    public val defaultBanReason: String = "Banned by an administrator",
    public val defaultMuteReason: String = "Muted by an administrator",
    public val maxReasonLength: Int = 256,
    public val maxTemporaryBanSeconds: Long? = null,
    /** Cap for a single mute duration; `null` disables the cap. Bypassed by `MUTE_UNLIMITED`. */
    public val maxMuteSeconds: Long? = null,
    public val notifyModerators: Boolean = true,
    public val auditEnabled: Boolean = true,
    /**
     * Login PvP protection window in seconds. A player who joined less than this many seconds ago
     * may not deal damage; `0` disables the guard. Bypassed by `PVP_DELAY_EXEMPT`.
     */
    public val loginAttackDelaySeconds: Int = 5,
    /**
     * Command roots a muted player may not run. Matched on the canonical CellulosesZ command name;
     * `"*"` blocks every command. Empty disables the gate.
     */
    public val mutedCommands: List<String> = listOf("msg", "reply", "mail", "me", "helpop"),
    /** Whether social spy also reports command executions (root command only). */
    public val socialSpyCommands: Boolean = true,
)
