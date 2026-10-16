package top.likoslupus.cellulosesz.api.communication

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle

public sealed interface AnnouncementOutcome {

    public data class Delivered(public val recipientCount: Int) : AnnouncementOutcome

    public data object NoRecipients : AnnouncementOutcome

    public data object NotAuthorized : AnnouncementOutcome

    public data object Disabled : AnnouncementOutcome

    public data object InvalidBody : AnnouncementOutcome

}

/**
 * Server-wide announcements. [access] must be authorized for `announcement.broadcast`; the sender
 * label is derived from the addon's metadata, never a caller-supplied "Console". Bodies are rendered
 * per recipient language and never treat user input as formatting.
 */
public interface AnnouncementsApi {

    public fun broadcast(
        access: AddonAccess,
        body: String,
        callback: ApiCallback<AnnouncementOutcome>,
    ): ApiRequestHandle

    public fun broadcastInDimension(
        access: AddonAccess,
        dimensionId: String,
        body: String,
        callback: ApiCallback<AnnouncementOutcome>,
    ): ApiRequestHandle

}
