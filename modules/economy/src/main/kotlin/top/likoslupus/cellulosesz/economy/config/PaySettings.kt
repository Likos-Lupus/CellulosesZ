package top.likoslupus.cellulosesz.economy.config

import kotlinx.serialization.Serializable

/**
 * Player-to-player payment policy. The three flags only supply defaults and gates for `/pay`; the
 * per-account toggles live in the shared preference store.
 */
@Serializable
public data class PaySettings(
    /** Whether a player accepts payments until they change it with `/paytoggle`. */
    public val acceptByDefault: Boolean = true,
    /** Whether `/pay` requires repeating the identical command before it executes. */
    public val confirmByDefault: Boolean = false,
    /** Whether a payment to a player who ignores the payer is refused. */
    public val excludesIgnoreList: Boolean = false,
)
