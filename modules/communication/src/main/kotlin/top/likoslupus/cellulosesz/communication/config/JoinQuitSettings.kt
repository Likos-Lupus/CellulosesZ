package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

/**
 * Custom join/quit announcements. Templates are strict MiniMessage; the supported tokens are
 * `{PLAYER}`, `{USERNAME}`, `{DISPLAYNAME}`, `{PREVIOUS_NAME}`, `{WORLD}`, `{ONLINE}`, `{UNIQUE}`,
 * `{UPTIME}` and `{GROUP}`. `{PREFIX}`/`{SUFFIX}` are intentionally not implemented (no
 * permission-plugin prefix API exists here).
 */
@Serializable
public data class JoinQuitSettings(
    public val enabled: Boolean = true,
    public val joinFormat: String =
        "<yellow>{PLAYER} joined the game</yellow>",
    public val quitFormat: String =
        "<yellow>{PLAYER} left the game</yellow>",
    public val firstJoinFormat: String =
        "<gold>{PLAYER} joined for the first time</gold>",
    public val newUsernameFormat: String =
        "<gray>{PLAYER} (previously {PREVIOUS_NAME}) joined the game</gray>",

    /** When true, a player with `cellulosesz.silentjoin` joins and quits without an announcement. */
    public val allowSilentJoinQuit: Boolean = true,

    /** Suppresses every join/quit announcement while more than this many players are online. 0 disables. */
    public val hideAbovePlayerCount: Int = 0,
)
