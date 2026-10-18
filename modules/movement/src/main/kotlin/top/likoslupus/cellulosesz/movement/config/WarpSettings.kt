package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/**
 * Warp access policy. [perWarpPermission] turns on a per-warp node `cellulosesz.warp.<name>`; when
 * enabled, `/warp` and `/warps` only offer warps the player may use.
 */
@Serializable
public data class WarpSettings(
    public val perWarpPermission: Boolean = false,
)
