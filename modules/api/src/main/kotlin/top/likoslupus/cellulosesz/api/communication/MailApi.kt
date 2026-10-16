package top.likoslupus.cellulosesz.api.communication

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Duration
import java.util.*

public class MailSummaryView(
    public val total: Int,
    public val unread: Int,
)

public class SendPlayerMailRequest(
    public val senderId: UUID,
    public val targetId: UUID,
    public val body: String,
    public val expiresAfter: Duration?,
)

/** A system-originated mail. [access] must be authorized for the `mail.send.system` capability. */
public class SendSystemMailRequest(
    public val access: AddonAccess,
    public val targetId: UUID,
    public val title: String?,
    public val body: String,
    public val deduplicationKey: String?,
)

public sealed interface MailSummaryOutcome {

    public data class Summary(public val summary: MailSummaryView) : MailSummaryOutcome

    public data object NotAuthorized : MailSummaryOutcome

    public data object PlayerUnknown : MailSummaryOutcome

    public data object StorageUnavailable : MailSummaryOutcome

}

public sealed interface MailSendOutcome {

    /**
     * Accepted and processed under the system's privacy policy. This does **not** guarantee a new
     * mailbox entry: an ignored sender is silently accepted without delivery.
     */
    public data object Accepted : MailSendOutcome

    public data object TargetUnknown : MailSendOutcome

    public data object Rejected : MailSendOutcome

    public data object MailboxFull : MailSendOutcome

    public data object RateLimited : MailSendOutcome

    public data object InvalidBody : MailSendOutcome

    public data object StorageUnavailable : MailSendOutcome

}

/**
 * Offline mail through the existing mail service: capacity, rate limiting, expiry and ignore privacy
 * are unchanged. Player-sent and system-sent mail are distinct operations, and the summary never
 * exposes message bodies.
 */
public interface MailApi {

    public fun summary(
        ownerId: UUID,
        callback: ApiCallback<MailSummaryOutcome>,
    ): ApiRequestHandle

    public fun sendAsPlayer(
        request: SendPlayerMailRequest,
        callback: ApiCallback<MailSendOutcome>,
    ): ApiRequestHandle

    public fun sendAsSystem(
        request: SendSystemMailRequest,
        callback: ApiCallback<MailSendOutcome>,
    ): ApiRequestHandle

}
