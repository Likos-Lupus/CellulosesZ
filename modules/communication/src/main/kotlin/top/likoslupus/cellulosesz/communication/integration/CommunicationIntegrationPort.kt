package top.likoslupus.cellulosesz.communication.integration

import java.time.Duration
import java.util.UUID

/**
 * Outcome of a private-message operation expressed at the domain boundary. It is a *result*, not a
 * verdict: a refusal (muted sender, hidden recipient, disabled feature) is a normal completion.
 */
public sealed interface PrivateMessagePortOutcome {

    /**
     * Delivered to [recipientId]. For a reply this is the resolved conversation partner, which the
     * caller cannot know in advance.
     */
    public data class Delivered(public val recipientId: UUID) : PrivateMessagePortOutcome

    public data object SenderOffline : PrivateMessagePortOutcome

    /** Covers a hidden/ignoring/absent recipient without revealing which — avoids a side channel. */
    public data object RecipientUnavailable : PrivateMessagePortOutcome

    public data object SenderRestricted : PrivateMessagePortOutcome

    public data object InvalidBody : PrivateMessagePortOutcome

    public data object Disabled : PrivateMessagePortOutcome

}

/** Outcome of reading a mailbox summary. It never carries message bodies. */
public sealed interface MailSummaryPortOutcome {

    public data class Summary(
        public val total: Int,
        public val unread: Int,
    ) : MailSummaryPortOutcome

    public data object PlayerUnknown : MailSummaryPortOutcome

    public data object StorageUnavailable : MailSummaryPortOutcome

}

/** Outcome of accepting a mail message. [Accepted] never implies a stored entry (privacy policy). */
public sealed interface MailSendPortOutcome {

    public data object Accepted : MailSendPortOutcome

    public data object TargetUnknown : MailSendPortOutcome

    public data object Rejected : MailSendPortOutcome

    public data object MailboxFull : MailSendPortOutcome

    public data object RateLimited : MailSendPortOutcome

    public data object InvalidBody : MailSendPortOutcome

    public data object StorageUnavailable : MailSendPortOutcome

    public data object Disabled : MailSendPortOutcome

}

/** Outcome of a server-wide announcement. */
public sealed interface AnnouncementPortOutcome {

    public data class Delivered(public val recipientCount: Int) : AnnouncementPortOutcome

    public data object NoRecipients : AnnouncementPortOutcome

    public data object Disabled : AnnouncementPortOutcome

    public data object InvalidBody : AnnouncementPortOutcome

}

/**
 * The narrow integration seam the application adapter uses to reach the communication bounded
 * context. It is deliberately addon-agnostic: authorization, capability checks and the public API
 * projection all live in the adapter, so this port has no dependency on `modules/api`, on the
 * loader, or on the administration `CommunicationIntegration`.
 *
 * Every exposed function delegates to an existing service. The port only resolves identifiers,
 * hops to the server thread where the service requires it, and maps the service's internal result
 * into a stable domain outcome. Mute, ignore, visibility, rate-limit, capacity, reply state and
 * expiry rules are therefore identical to the commands.
 */
public interface CommunicationIntegrationPort {

    /** Sends a private message from [senderId] to [recipientId] through the existing pipeline. */
    public suspend fun sendPrivateMessage(
        senderId: UUID,
        recipientId: UUID,
        body: String,
    ): PrivateMessagePortOutcome

    /** Replies to the sender's last conversation partner, preserving reply state. */
    public suspend fun replyPrivateMessage(
        senderId: UUID,
        body: String,
    ): PrivateMessagePortOutcome

    /**
     * Whether [playerId] currently accepts private messages. `null` means there is no loaded basis
     * for an answer (the player has no cached preference snapshot). Server-thread confined, like the
     * other cached reads in this feature.
     */
    public fun receivingEnabled(playerId: UUID): Boolean?

    /** Reads a mailbox summary; never exposes message bodies. */
    public suspend fun mailSummary(ownerId: UUID): MailSummaryPortOutcome

    /** Player-originated mail; subject to sender gate, rate limiting, capacity and expiry. */
    public suspend fun sendPlayerMail(
        senderId: UUID,
        targetId: UUID,
        body: String,
        expiresAfter: Duration?,
    ): MailSendPortOutcome

    /**
     * System-originated mail. The caller must already be authorized for the `mail.send.system`
     * capability; the port only performs delivery.
     */
    public suspend fun sendSystemMail(
        targetId: UUID,
        title: String?,
        body: String,
        deduplicationKey: String?,
    ): MailSendPortOutcome

    /**
     * Broadcasts a literal announcement to every online player. [sourceId] is the addon-derived
     * sender label recorded in the server log; it is never caller-supplied text.
     */
    public suspend fun broadcast(
        sourceId: String,
        body: String,
    ): AnnouncementPortOutcome

    /** Broadcasts a literal announcement to one dimension, identified by its namespaced id. */
    public suspend fun broadcastInDimension(
        sourceId: String,
        dimensionId: String,
        body: String,
    ): AnnouncementPortOutcome

}
