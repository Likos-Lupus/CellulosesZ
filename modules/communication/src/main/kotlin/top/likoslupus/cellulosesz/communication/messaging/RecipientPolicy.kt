package top.likoslupus.cellulosesz.communication.messaging

import top.likoslupus.cellulosesz.communication.config.ReplyMode
import java.util.*

internal enum class ReceiveDecision {

    ALLOWED,
    DENIED,
    LOADING,

}

/**
 * Server-thread confined view of a recipient's messaging policy. Implemented by the preferences
 * service so private-message delivery can stay pure and unit testable.
 */
internal interface RecipientPolicy {

    fun canReceiveFrom(
        recipientId: UUID,
        senderId: UUID,
    ): ReceiveDecision

    fun replyMode(playerId: UUID): ReplyMode

    /**
     * Whether [ignorerId] has [targetId] on their ignore list. Used by broadcast-style features
     * (`/me`) so a sender's own ignore list hides their action lines. Permissive by default.
     */
    fun ignores(ignorerId: UUID, targetId: UUID): Boolean = false

}
