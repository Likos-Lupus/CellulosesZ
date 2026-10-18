package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/**
 * Runtime policy for the jail capability. `defaultJail` names the location used by `/jail` and
 * `/togglejail` when the player is not already jailed; a blank value means no configured default.
 */
@Serializable
public data class JailSettings(
    public val enabled: Boolean = true,
    public val defaultJail: String = "",
    public val maxJails: Int = 32,
    public val allowedRadius: Double = 8.0,
    public val returnOnRelease: Boolean = true,
    public val confinementCheckIntervalTicks: Int = 20,
    public val restrictCommands: Boolean = false,
    public val allowedCommands: List<String> = listOf("helpop"),
    /**
     * When true, a timed sentence counts only the seconds the player is online: the remaining time
     * is stored in `cz_jail_sentences.remaining_seconds` and decremented by a ticker. When false, a
     * timed sentence expires on wall-clock time.
     */
    public val jailOnlineTime: Boolean = false,
)
