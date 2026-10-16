package top.likoslupus.cellulosesz.api.presentation

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.util.*

/** A translation key owned by one addon. Keys are namespaced so an addon cannot shadow core keys. */
public class AddonTextKey(
    public val addonId: String,
    public val key: String,
)

/**
 * A message the addon wants rendered. [arguments] are substituted as **literal** text; they are never
 * interpreted as MiniMessage/Markdown or as clickable/executable components.
 */
public class AddonTextMessage(
    public val key: AddonTextKey,
    public val arguments: List<String>,
)

public sealed interface PresentationOutcome {

    public data object Sent : PresentationOutcome

    public data object RecipientOffline : PresentationOutcome

    public data object UnknownKey : PresentationOutcome

    public data object NotAuthorized : PresentationOutcome

    public data object Disabled : PresentationOutcome

}

/**
 * A convenience for delivering an addon's own localized notice through CellulosesZ's per-recipient
 * language/theme pipeline. Addons may equally render their own Adventure message; this is optional.
 */
public interface AddonPresentationApi {

    public fun sendNotice(
        access: AddonAccess,
        recipientId: UUID,
        message: AddonTextMessage,
        callback: ApiCallback<PresentationOutcome>,
    ): ApiRequestHandle

}
