package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/**
 * Per-world teleport permission gate. When disabled (the default) every cross-world destination is
 * allowed; when enabled, the teleport subject must hold `cellulosesz.world.<dimension>` for the
 * destination world.
 */
@Serializable
public data class WorldAccessSettings(
    public val enabled: Boolean = false,
)
