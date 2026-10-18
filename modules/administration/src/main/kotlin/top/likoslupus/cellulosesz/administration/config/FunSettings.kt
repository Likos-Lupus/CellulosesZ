package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/**
 * The playful commands (`/antioch`, `/beezooka`, `/kittycannon`). `enabled = false` removes the
 * whole group from the command registry.
 */
@Serializable
public data class FunSettings(
    public val enabled: Boolean = true,
)
