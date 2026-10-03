package top.likoslupus.cellulosesz.communication

import java.util.*

/** Whether a sender is allowed to originate a private message. */
public fun interface PrivateMessageSenderGate {

    public fun check(senderId: UUID): SendGateResult

}

/** Whether a sender may reach a target, for example because the target is vanished. */
public fun interface PrivateMessageReachability {

    public fun canReach(senderId: UUID, targetId: UUID): Boolean

}

/** Observes a successfully delivered private message. */
public fun interface PrivateMessageObserver {

    public fun delivered(observation: PrivateMessageObservation)

}

/**
 * The narrow cross-feature capabilities the composition root injects into communication. It is not
 * an event bus: administration (mute), vanish and social spy are all expressed through these three
 * typed seams, and communication never imports administration.
 */
public data class CommunicationIntegration(
    public val senderGate: PrivateMessageSenderGate =
        PrivateMessageSenderGate { SendGateResult.Allowed },
    public val targetReachability: PrivateMessageReachability =
        PrivateMessageReachability { _, _ -> true },
    public val observer: PrivateMessageObserver =
        PrivateMessageObserver { },
)
