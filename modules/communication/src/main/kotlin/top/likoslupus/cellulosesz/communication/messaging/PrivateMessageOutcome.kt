package top.likoslupus.cellulosesz.communication.messaging

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody

internal sealed interface PrivateMessageOutcome {

    data class Delivered(
        val target: OnlinePlayerIdentity,
        val body: MessageBody,
    ) : PrivateMessageOutcome

    data object Disabled : PrivateMessageOutcome
    data object InvalidMessage : PrivateMessageOutcome
    data object SenderRequired : PrivateMessageOutcome
    data object SenderOffline : PrivateMessageOutcome
    data class SenderRestricted(val feedback: Component) : PrivateMessageOutcome
    data class TargetOffline(val name: String?) : PrivateMessageOutcome
    data object SelfTarget : PrivateMessageOutcome
    data object NoReplyTarget : PrivateMessageOutcome
    data object RecipientUnavailable : PrivateMessageOutcome
    data object RecipientStateLoading : PrivateMessageOutcome

}
