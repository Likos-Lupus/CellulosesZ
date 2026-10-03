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

}
