package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

/** Controls whether successful teleports record the previous location for `/back`. */
@Serializable
public data class HistorySettings(
    public val enabled: Boolean = true,
)
