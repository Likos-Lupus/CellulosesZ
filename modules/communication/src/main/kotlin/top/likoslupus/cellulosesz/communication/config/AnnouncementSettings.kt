package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

@Serializable
public data class AnnouncementSettings(
    public val enabled: Boolean = true,
    public val maxMessageLength: Int = 1000,
)
