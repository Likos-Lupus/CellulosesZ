package top.likoslupus.cellulosesz.api.communication

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import java.util.*

/** A coarse delivery notification. It never contains message bodies or ignore/presence detail. */
public class PrivateMessageDelivered(
    public val senderId: UUID?,
    public val recipientId: UUID,
)

public enum class MailSenderKind {

    PLAYER,
    SYSTEM,
    ADDON,

}

/** Mail was accepted and handled under the privacy policy (not necessarily stored). */
public class MailAccepted(
    public val recipientId: UUID,
    public val senderKind: MailSenderKind,
    public val messageId: String?,
)

public class AnnouncementDelivered(
    public val sourceId: String,
    public val recipientCount: Int,
    public val dimensionId: String?,
)

/** Communication fact events; minimal metadata, dispatched on the server thread. */
public interface CommunicationEvents {

    public fun onPrivateMessageDelivered(
        listener: ApiEventListener<PrivateMessageDelivered>,
    ): Subscription

    public fun onMailAccepted(
        listener: ApiEventListener<MailAccepted>,
    ): Subscription

    public fun onAnnouncementDelivered(
        listener: ApiEventListener<AnnouncementDelivered>,
    ): Subscription

}
