package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

@Serializable
public data class PrivateMessageSettings(
    public val enabled: Boolean = true,
    public val maxMessageLength: Int = 512,
    public val maxIgnoredPlayers: Int = 128,
    public val defaultReplyMode: ReplyMode = ReplyMode.LAST_INTERACTION,

    /**
     * `null` means session reply state does not expire by time.
     */
    public val replyTimeoutSeconds: Long? = null,
)
