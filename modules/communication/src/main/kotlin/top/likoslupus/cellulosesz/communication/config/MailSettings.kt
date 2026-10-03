package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

@Serializable
public data class MailSettings(
    public val enabled: Boolean = true,
    public val maxMessageLength: Int = 1000,
    public val maxMessagesPerMailbox: Int = 100,
    public val maxSendsPerMinute: Int = 10,
    public val notifyOnJoin: Boolean = true,

    /**
     * `null` means no server maximum for temporary mail.
     */
    public val maxTemporaryMailSeconds: Long? = null,
)
