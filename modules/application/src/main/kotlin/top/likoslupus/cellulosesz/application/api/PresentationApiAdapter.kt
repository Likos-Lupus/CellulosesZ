package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.presentation.*
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import java.util.*

/** The one capability an addon must be granted before it may push its own notices through the host. */
private const val PRESENTATION_NOTICE_CAPABILITY = "presentation.notice"

/**
 * The Presentation domain adapter. Every sub-facade is a thin seam over an existing host service:
 *
 * - [language] reads the deterministic, server-owned effective language without touching the client
 *   locale ([LanguageResolver.effective]).
 * - [addon] renders an addon-owned keyed message through the semantic [TextPresentation] notice
 *   pipeline and delivers it on the server thread. Message arguments are bound as
 *   [TextArgument.Literal], so addon text can never smuggle MiniMessage/Markdown or click actions.
 * - [help] delegates registration to the extension hub's `registerHelpDocument` (injected), which
 *   owns the contribution window and duplicate-key policy.
 *
 * The view is bound to one [SessionLease]; an old session's view returns [ApiQuery.Stopping] forever
 * and is never revived by a later session.
 *
 * ## Addon text catalog
 *
 * [resolveAddonText] is backed by the runtime [top.likoslupus.cellulosesz.core.text.i18n.MessageOverlay]
 * that collects addon-registered translations. When it cannot resolve a key,
 * [AddonPresentationApi.sendNotice] honestly reports [PresentationOutcome.UnknownKey] instead of
 * fabricating or rendering a `<missing:...>` marker.
 */
internal class PresentationApiAdapter(
    private val lease: SessionLease,
    languages: LanguageResolver,
    presentation: TextPresentation,
    adventure: AdventureRuntime,
    kernel: RuntimeKernel,
    dispatcher: ApiRequestDispatcher,
    isOnline: (UUID) -> Boolean,
    registerHelp: (AddonAccess, HelpEntryDescriptor) -> HelpContributionResult,
    resolveAddonText: (AddonTextKey, LanguageId) -> LocalizedText? = { _, _ -> null },
) : PresentationApi {

    override val language: PlayerLanguageApi = PlayerLanguageAdapter(lease, languages)

    override val addon: AddonPresentationApi = AddonPresentationAdapter(
        lease = lease,
        languages = languages,
        presentation = presentation,
        adventure = adventure,
        kernel = kernel,
        dispatcher = dispatcher,
        isOnline = isOnline,
        resolveAddonText = resolveAddonText,
    )

    override val help: HelpContributionApi = HelpContributionAdapter(registerHelp)

}

/** The effective language a player sees; a pure snapshot with no client-locale guessing. */
private class PlayerLanguageAdapter(
    private val lease: SessionLease,
    private val languages: LanguageResolver,
) : PlayerLanguageApi {

    override fun effective(playerId: UUID): ApiQuery<String> =
        when (lease.phase()) {
            SessionPhase.READY -> ApiQuery.Available(languages.effective(playerId).value)
            SessionPhase.CREATED -> ApiQuery.NotReady
            SessionPhase.CLOSED -> ApiQuery.Stopping
        }

}

/**
 * Renders an addon-owned keyed message to the recipient's language and delivers it. Authorization is
 * a live check at execution time, so a revoked capability is never honoured from a stale grant.
 *
 * Outcome precedence: [PresentationOutcome.NotAuthorized] (capability), [PresentationOutcome.UnknownKey]
 * (not this addon's key, or the catalog cannot resolve it), [PresentationOutcome.RecipientOffline],
 * then [PresentationOutcome.Sent].
 */
private class AddonPresentationAdapter(
    private val lease: SessionLease,
    private val languages: LanguageResolver,
    private val presentation: TextPresentation,
    private val adventure: AdventureRuntime,
    private val kernel: RuntimeKernel,
    private val dispatcher: ApiRequestDispatcher,
    private val isOnline: (UUID) -> Boolean,
    private val resolveAddonText: (AddonTextKey, LanguageId) -> LocalizedText?,
) : AddonPresentationApi {

    override fun sendNotice(
        access: AddonAccess,
        recipientId: UUID,
        message: AddonTextMessage,
        callback: ApiCallback<PresentationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = { decide(access, recipientId, message) },
        deliver = callback::onComplete,
    )

    private suspend fun decide(
        access: AddonAccess,
        recipientId: UUID,
        message: AddonTextMessage,
    ): ApiCallResult<PresentationOutcome> =
        when (lease.phase()) {
            SessionPhase.CREATED -> ApiCallResult.NotReady
            SessionPhase.CLOSED -> ApiCallResult.Stopping
            SessionPhase.READY -> deliver(access, recipientId, message)
        }

    private suspend fun deliver(
        access: AddonAccess,
        recipientId: UUID,
        message: AddonTextMessage,
    ): ApiCallResult<PresentationOutcome> {
        if (!access.isAllowed(PRESENTATION_NOTICE_CAPABILITY)) {
            return ApiCallResult.Completed(PresentationOutcome.NotAuthorized)
        }
        // Keys are namespaced by owner: an addon may only render its own text, never shadow core or
        // another addon's keys.
        if (message.key.addonId != access.addonId) {
            return ApiCallResult.Completed(PresentationOutcome.UnknownKey)
        }
        val language = languages.effective(recipientId)
        val text = resolveAddonText(message.key, language)
            ?: return ApiCallResult.Completed(PresentationOutcome.UnknownKey)
        if (!isOnline(recipientId)) {
            return ApiCallResult.Completed(PresentationOutcome.RecipientOffline)
        }
        kernel.sendNotices(
            target = CommandFeedbackTarget.Player(recipientId),
            adventure = adventure,
            presentation = presentation,
            playerId = recipientId,
            notices = listOf(
                Notice(
                    text = LocalizedText(
                        key = text.key,
                        arguments = message.arguments.map { TextArgument.Literal(it) },
                    ),
                )
            ),
        )
        return ApiCallResult.Completed(PresentationOutcome.Sent)
    }

}

/**
 * Forwards a help-document contribution to the injected host registration function. The hub owns the
 * contribution window, duplicate-key and key-format policy, so those results pass through unchanged.
 */
private class HelpContributionAdapter(
    private val registerHelp: (AddonAccess, HelpEntryDescriptor) -> HelpContributionResult,
) : HelpContributionApi {

    override fun register(
        access: AddonAccess,
        entry: HelpEntryDescriptor,
    ): HelpContributionResult =
        registerHelp(access, entry)

}
