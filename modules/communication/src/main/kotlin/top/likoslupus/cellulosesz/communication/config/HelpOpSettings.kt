package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

@Serializable
public data class HelpOpSettings(
    public val enabled: Boolean = true,
    public val maxMessageLength: Int = 512,
    public val maxMessagesPerMinute: Int = 6,
)
