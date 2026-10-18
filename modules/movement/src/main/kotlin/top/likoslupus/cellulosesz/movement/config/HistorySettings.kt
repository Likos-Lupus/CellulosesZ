package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/** Controls whether successful teleports record the previous location for `/back`. */
@Serializable
public data class HistorySettings(
    public val enabled: Boolean = true,

    /**
     * Records the death location so `/back` can return there. The player still needs
     * `cellulosesz.back.ondeath` for `/back` to prefer it.
     */
    public val recordDeath: Boolean = true,
)
