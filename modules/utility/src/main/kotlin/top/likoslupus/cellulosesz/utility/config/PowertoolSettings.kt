package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable

/** Product policy for the `/powertool` interaction bindings. */
@Serializable
public data class PowertoolSettings(
    public val enabled: Boolean = true,
    /** Minimum seconds between two powertool firings for the same player. */
    public val throttleSeconds: Int = 1,
)
