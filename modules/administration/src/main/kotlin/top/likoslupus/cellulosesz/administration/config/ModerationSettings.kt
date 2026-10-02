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
    public val maxTemporaryMuteSeconds: Long? = null,
    public val notifyModerators: Boolean = true,
    public val auditEnabled: Boolean = true,
)
