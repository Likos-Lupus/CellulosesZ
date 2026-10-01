package top.likoslupus.cellulosesz.movement.config

import kotlinx.serialization.Serializable

@Serializable
public data class HomeSettings(
    public val maxPerPlayer: Int = 10,
    public val defaultName: String = "home",
)
