package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

/**
 * Death message handling. When [enabled] and [customFormat] is set, the vanilla death message is
 * suppressed and replaced; when [customFormat] is absent the vanilla message is left untouched.
 * [showCoordinates] additionally tells the dead player where they died.
 *
 * `customFormat` supports `{PLAYER}`, `{USERNAME}`, `{DISPLAYNAME}`, `{WORLD}`, `{X}`, `{Y}`, `{Z}`
 * and `{MESSAGE}` (the vanilla death message, resolved server-side). `coordinatesFormat` supports
 * `{WORLD}`, `{X}`, `{Y}` and `{Z}`.
 */
@Serializable
public data class DeathMessageSettings(
    public val enabled: Boolean = true,

    /** Optional strict MiniMessage replacement for the vanilla death message. */
    public val customFormat: String? = null,

    public val showCoordinates: Boolean = true,

    /** Strict MiniMessage line sent to the dead player. A blank value falls back to the localized default. */
    public val coordinatesFormat: String = "<gray>You died at <white>{X}, {Y}, {Z}</white> in {WORLD}</gray>",
)
