package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/**
 * One permission-scoped home allowance. The highest matching entry wins; [maxPerPlayer] is the
 * default when no entry's permission is held.
 */
@Serializable
public data class HomeLimitSettings(
    public val permission: String,
    public val max: Int,
)

@Serializable
public data class HomeSettings(
    public val maxPerPlayer: Int = 10,
    public val defaultName: String = "home",
    public val limits: List<HomeLimitSettings> = emptyList(),
    /** Requires `cellulosesz.world.home.<dimension>` to teleport to a home in another world. */
    public val worldHomePermissions: Boolean = false,
    /** When true, `/sethome` on an existing name asks for a second confirmation before overwriting. */
    public val confirmHomeOverwrite: Boolean = true,
)
