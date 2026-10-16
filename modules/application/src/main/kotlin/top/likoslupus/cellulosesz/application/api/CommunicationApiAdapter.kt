package top.likoslupus.cellulosesz.application.api

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.communication.*
import top.likoslupus.cellulosesz.communication.integration.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Adapts the communication feature's [CommunicationIntegrationPort] onto the public
 * [CommunicationApi]. Every call is dispatched through [ApiRequestDispatcher] so the outcome is
 * delivered once, on the server thread, while a session is active. Expected business refusals stay
 * inside the domain outcome; only runtime states become `NotReady`/`Stopping`/`Unavailable`/`Failed`.
 *
 * A fresh adapter (and therefore a fresh [CommunicationEventSink]) belongs to exactly one server
 * session. The composition root may keep the sink and dispatch observed facts to it; the adapter
 * already dispatches the deliveries it performs so the API is self-consistent on its own.
 */
internal class CommunicationApiAdapter(
    private val port: CommunicationIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
    private val sink: CommunicationEventSink = CommunicationEventSink(),
) : CommunicationApi {

    override val privateMessages: PrivateMessagesApi =
        PrivateMessagesAdapter(port, dispatcher, sink)

    override val mail: MailApi =
        MailAdapter(port, dispatcher, sink)

    override val announcements: AnnouncementsApi =
        AnnouncementsAdapter(port, dispatcher, sink)

    override val events: CommunicationEvents
        get() = sink

    /**
     * The session-scoped event fan-out. The composition root may dispatch facts observed outside the
     * API (for example a delivered in-game private message) here, so addon listeners see them too.
     */
    fun eventSink(): CommunicationEventSink = sink

}

private const val CAPABILITY_MAIL_SEND_SYSTEM: String = "mail.send.system"
private const val CAPABILITY_ANNOUNCEMENT_BROADCAST: String = "announcement.broadcast"

/**
 * Session-scoped fan-out for communication facts. A throwing listener is isolated and never
 * prevents the others; [clear] makes the sink inert so an old session's listeners can never be
 * revived by a later one.
 */
internal class CommunicationEventSink : CommunicationEvents {

    private val privateMessageListeners =
        CopyOnWriteArrayList<ApiEventListener<PrivateMessageDelivered>>()

    private val mailListeners =
        CopyOnWriteArrayList<ApiEventListener<MailAccepted>>()

    private val announcementListeners =
        CopyOnWriteArrayList<ApiEventListener<AnnouncementDelivered>>()

    @Volatile private var active: Boolean = true

    override fun onPrivateMessageDelivered(
        listener: ApiEventListener<PrivateMessageDelivered>,
    ): Subscription =
        register(privateMessageListeners, listener)

    override fun onMailAccepted(
        listener: ApiEventListener<MailAccepted>,
    ): Subscription =
        register(mailListeners, listener)

    override fun onAnnouncementDelivered(
        listener: ApiEventListener<AnnouncementDelivered>,
    ): Subscription =
        register(announcementListeners, listener)

    fun dispatchPrivateMessageDelivered(event: PrivateMessageDelivered) {
        if (!active) {
            return
        }
        privateMessageListeners.forEach { listener ->
            runCatching { listener.onEvent(event) }
                    .onFailure {
                        LOGGER.warn("api_private_message_listener_failed", it)
                    }
        }
    }

    fun dispatchMailAccepted(event: MailAccepted) {
        if (!active) {
            return
        }
        mailListeners.forEach { listener ->
            runCatching { listener.onEvent(event) }
                    .onFailure {
                        LOGGER.warn("api_mail_listener_failed", it)
                    }
        }
    }

    fun dispatchAnnouncementDelivered(event: AnnouncementDelivered) {
        if (!active) {
            return
        }
        announcementListeners.forEach { listener ->
            runCatching { listener.onEvent(event) }
                    .onFailure {
                        LOGGER.warn("api_announcement_listener_failed", it)
                    }
        }
    }

    /** Drops every listener and makes this sink inert. Called when its server session ends. */
    fun clear() {
        active = false
        privateMessageListeners.clear()
        mailListeners.clear()
        announcementListeners.clear()
    }

    private fun <T> register(
        listeners: MutableList<ApiEventListener<T>>,
        listener: ApiEventListener<T>,
    ): Subscription {
        if (!active) {
            return Subscription { }
        }
        listeners += listener
        return Subscription { listeners.remove(listener) }
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(CommunicationEventSink::class.java)

    }

}

private class PrivateMessagesAdapter(
    private val port: CommunicationIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
    private val sink: CommunicationEventSink,
) : PrivateMessagesApi {

    override fun sendAsPlayer(
        request: PrivateMessageRequest,
        callback: ApiCallback<PrivateMessageOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = {
                when (
                    val outcome = port.sendPrivateMessage(
                        request.senderId,
                        request.recipientId,
                        request.body,
                    )
                ) {
                    is PrivateMessagePortOutcome.Delivered -> {
                        sink.dispatchPrivateMessageDelivered(
                            PrivateMessageDelivered(request.senderId, outcome.recipientId)
                        )
                        ApiCallResult.Completed(PrivateMessageOutcome.Delivered)
                    }

                    else -> ApiCallResult.Completed(outcome.toApiOutcome())
                }
            },
            deliver = callback::onComplete,
        )

    override fun replyAsPlayer(
        senderId: UUID,
        body: String,
        callback: ApiCallback<PrivateMessageOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = {
                when (val outcome = port.replyPrivateMessage(senderId, body)) {
                    is PrivateMessagePortOutcome.Delivered -> {
                        sink.dispatchPrivateMessageDelivered(
                            PrivateMessageDelivered(senderId, outcome.recipientId)
                        )
                        ApiCallResult.Completed(PrivateMessageOutcome.Delivered)
                    }

                    else -> ApiCallResult.Completed(outcome.toApiOutcome())
                }
            },
            deliver = callback::onComplete,
        )

    override fun receivingEnabled(playerId: UUID): ApiQuery<Boolean?> =
        ApiQuery.Available(port.receivingEnabled(playerId))

}

private class MailAdapter(
    private val port: CommunicationIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
    private val sink: CommunicationEventSink,
) : MailApi {

    override fun summary(
        ownerId: UUID,
        callback: ApiCallback<MailSummaryOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = {
                when (val outcome = port.mailSummary(ownerId)) {
                    is MailSummaryPortOutcome.Summary -> ApiCallResult.Completed(
                        MailSummaryOutcome.Summary(
                            MailSummaryView(outcome.total, outcome.unread)
                        )
                    )

                    MailSummaryPortOutcome.PlayerUnknown ->
                        ApiCallResult.Completed(MailSummaryOutcome.PlayerUnknown)

                    MailSummaryPortOutcome.StorageUnavailable ->
                        ApiCallResult.Completed(MailSummaryOutcome.StorageUnavailable)
                }
            },
            deliver = callback::onComplete,
        )

    override fun sendAsPlayer(
        request: SendPlayerMailRequest,
        callback: ApiCallback<MailSendOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = {
                when (
                    val outcome = port.sendPlayerMail(
                        request.senderId,
                        request.targetId,
                        request.body,
                        request.expiresAfter,
                    )
                ) {
                    MailSendPortOutcome.Disabled -> ApiCallResult.Unavailable("mail")

                    MailSendPortOutcome.Accepted -> {
                        sink.dispatchMailAccepted(
                            MailAccepted(
                                request.targetId,
                                MailSenderKind.PLAYER,
                                null
                            )
                        )
                        ApiCallResult.Completed(MailSendOutcome.Accepted)
                    }

                    else -> ApiCallResult.Completed(outcome.toApiOutcome())
                }
            },
            deliver = callback::onComplete,
        )

    override fun sendAsSystem(
        request: SendSystemMailRequest,
        callback: ApiCallback<MailSendOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = {
                when {
                    !request.access.isAllowed(CAPABILITY_MAIL_SEND_SYSTEM) ->
                        ApiCallResult.Completed(MailSendOutcome.Rejected)

                    else -> when (
                        val outcome = port.sendSystemMail(
                            request.targetId,
                            request.title,
                            request.body,
                            request.deduplicationKey,
                        )
                    ) {
                        MailSendPortOutcome.Disabled ->
                            ApiCallResult.Unavailable("mail")

                        MailSendPortOutcome.Accepted -> {
                            sink.dispatchMailAccepted(
                                MailAccepted(
                                    request.targetId,
                                    MailSenderKind.SYSTEM,
                                    null
                                )
                            )
                            ApiCallResult.Completed(MailSendOutcome.Accepted)
                        }

                        else -> ApiCallResult.Completed(outcome.toApiOutcome())
                    }
                }
            },
            deliver = callback::onComplete,
        )

}

private class AnnouncementsAdapter(
    private val port: CommunicationIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
    private val sink: CommunicationEventSink,
) : AnnouncementsApi {

    override fun broadcast(
        access: AddonAccess,
        body: String,
        callback: ApiCallback<AnnouncementOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = { deliverBroadcast(access, body, null) },
            deliver = callback::onComplete,
        )

    override fun broadcastInDimension(
        access: AddonAccess,
        dimensionId: String,
        body: String,
        callback: ApiCallback<AnnouncementOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = { deliverBroadcast(access, body, dimensionId) },
            deliver = callback::onComplete,
        )

    private suspend fun deliverBroadcast(
        access: AddonAccess,
        body: String,
        dimensionId: String?,
    ): ApiCallResult<AnnouncementOutcome> {
        if (!access.isAllowed(CAPABILITY_ANNOUNCEMENT_BROADCAST)) {
            return ApiCallResult.Completed(AnnouncementOutcome.NotAuthorized)
        }

        val outcome = when (dimensionId) {
            null -> port.broadcast(access.addonId, body)
            else -> port.broadcastInDimension(access.addonId, dimensionId, body)
        }

        return when (outcome) {
            is AnnouncementPortOutcome.Delivered -> {
                sink.dispatchAnnouncementDelivered(
                    AnnouncementDelivered(
                        access.addonId,
                        outcome.recipientCount,
                        dimensionId,
                    )
                )
                ApiCallResult.Completed(
                    AnnouncementOutcome.Delivered(outcome.recipientCount)
                )
            }

            AnnouncementPortOutcome.NoRecipients ->
                ApiCallResult.Completed(AnnouncementOutcome.NoRecipients)

            AnnouncementPortOutcome.Disabled ->
                ApiCallResult.Completed(AnnouncementOutcome.Disabled)

            AnnouncementPortOutcome.InvalidBody ->
                ApiCallResult.Completed(AnnouncementOutcome.InvalidBody)
        }
    }

}

private fun PrivateMessagePortOutcome.toApiOutcome(): PrivateMessageOutcome =
    when (this) {
        is PrivateMessagePortOutcome.Delivered -> PrivateMessageOutcome.Delivered
        PrivateMessagePortOutcome.SenderOffline -> PrivateMessageOutcome.SenderOffline
        PrivateMessagePortOutcome.RecipientUnavailable -> PrivateMessageOutcome.RecipientUnavailable
        PrivateMessagePortOutcome.SenderRestricted -> PrivateMessageOutcome.SenderRestricted
        PrivateMessagePortOutcome.InvalidBody -> PrivateMessageOutcome.InvalidBody
        PrivateMessagePortOutcome.Disabled -> PrivateMessageOutcome.Disabled
    }

private fun MailSendPortOutcome.toApiOutcome(): MailSendOutcome =
    when (this) {
        MailSendPortOutcome.Accepted -> MailSendOutcome.Accepted
        MailSendPortOutcome.TargetUnknown -> MailSendOutcome.TargetUnknown
        MailSendPortOutcome.Rejected -> MailSendOutcome.Rejected
        MailSendPortOutcome.MailboxFull -> MailSendOutcome.MailboxFull
        MailSendPortOutcome.RateLimited -> MailSendOutcome.RateLimited
        MailSendPortOutcome.InvalidBody -> MailSendOutcome.InvalidBody
        MailSendPortOutcome.StorageUnavailable -> MailSendOutcome.StorageUnavailable
        MailSendPortOutcome.Disabled -> MailSendOutcome.Rejected
    }
