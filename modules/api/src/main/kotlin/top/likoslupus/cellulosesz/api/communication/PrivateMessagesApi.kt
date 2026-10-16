package top.likoslupus.cellulosesz.api.communication

import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.*

public class PrivateMessageRequest(
    public val senderId: UUID,
    public val recipientId: UUID,
    public val body: String,
)

public sealed interface PrivateMessageOutcome {

    public data object Delivered : PrivateMessageOutcome

    public data object SenderOffline : PrivateMessageOutcome

    /** Covers a hidden/ignoring/absent recipient without revealing which — avoids a side channel. */
    public data object RecipientUnavailable : PrivateMessageOutcome

    public data object SenderRestricted : PrivateMessageOutcome

    public data object InvalidBody : PrivateMessageOutcome

    public data object Disabled : PrivateMessageOutcome

}

/**
 * Player-to-player messaging through the existing private-message pipeline: mute, visibility, ignore,
 * receiving preferences and reply state are all preserved. There is deliberately no "send as another
 * player" operation.
 */
public interface PrivateMessagesApi {

    public fun sendAsPlayer(
        request: PrivateMessageRequest,
        callback: ApiCallback<PrivateMessageOutcome>,
    ): ApiRequestHandle

    public fun replyAsPlayer(
        senderId: UUID,
        body: String,
        callback: ApiCallback<PrivateMessageOutcome>,
    ): ApiRequestHandle

    public fun receivingEnabled(playerId: UUID): ApiQuery<Boolean?>

}
