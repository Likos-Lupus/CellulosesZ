package top.likoslupus.cellulosesz.communication

/**
 * Emitted after a private message has been successfully delivered. The application forwards this to
 * administration; communication never learns about moderation.
 */
public data class PrivateMessageObservation(
    public val senderId: java.util.UUID,
    public val senderName: String,
    public val targetId: java.util.UUID,
    public val targetName: String,
    public val text: String,
)
