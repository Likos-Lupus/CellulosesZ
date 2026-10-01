package top.likoslupus.cellulosesz.communication

import kotlinx.serialization.Serializable

@Serializable
public data class MessagingSettings(
    public val enabled: Boolean = true,
)
