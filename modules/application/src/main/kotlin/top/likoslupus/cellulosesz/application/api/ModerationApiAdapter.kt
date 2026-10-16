package top.likoslupus.cellulosesz.application.api

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.administration.integration.*
import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.moderation.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

private const val CAPABILITY_MUTE = "moderation.mute"
private const val CAPABILITY_UNMUTE = "moderation.unmute"
private const val CAPABILITY_CONFINE = "moderation.confine"
private const val CAPABILITY_RELEASE = "moderation.release"

/**
 * The moderation facade over the administration integration port. One instance belongs to one server
 * session: reads are session-gated by the provider, actions run through [ApiRequestDispatcher] and
 * are authorized at the port (a player) or against the live addon capability snapshot (an addon),
 * and the [events] fan-out is fed by the composition root through [dispatch].
 */
internal class ModerationApiAdapter(
    private val port: ModerationIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : ModerationApi {

    private val fanOut = ModerationEventFanOut()

    override val status: ModerationStatusApi = ModerationStatusAdapter(port, dispatcher)

    override val actions: ModerationActionsApi = ModerationActionsAdapter(port, dispatcher)

    override val events: ModerationEvents = fanOut

    /** Routes a committed moderation fact to this session's listeners. */
    fun dispatch(event: ModerationDomainEvent) {
        fanOut.dispatch(event)
    }

    /** Drops this session's listeners; the composition root calls it when the session stops. */
    fun clear() {
        fanOut.clear()
    }

}

private class ModerationStatusAdapter(
    private val port: ModerationIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : ModerationStatusApi {

    override fun isMuted(viewerId: UUID, subjectId: UUID): ApiQuery<Boolean> =
        ApiQuery.Available(port.isMuted(viewerId, subjectId))

    override fun status(
        viewerId: UUID,
        subjectId: UUID,
        callback: ApiCallback<ModerationStatusOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = {
                when (val status = port.status(viewerId, subjectId)) {
                    is ModerationIntegrationStatus.RuntimeStopping -> ApiCallResult.Stopping
                    else -> ApiCallResult.Completed(status.toPublic())
                }
            },
            deliver = callback::onComplete,
        )

}

private class ModerationActionsAdapter(
    private val port: ModerationIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : ModerationActionsApi {

    override fun mute(
        request: ModerationRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle =
        submitAction(callback) {
            if (!gate(request.actor, CAPABILITY_MUTE)) {
                return@submitAction ApiCallResult.Completed(
                    ModerationActionOutcome.NotAuthorized
                )
            }
            port.mute(
                request.actor.toIntegration(),
                request.targetId,
                request.reason,
                request.duration,
            ).toCallResult()
        }

    override fun unmute(
        request: ModerationRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle =
        submitAction(callback) {
            if (!gate(request.actor, CAPABILITY_UNMUTE)) {
                return@submitAction ApiCallResult.Completed(
                    ModerationActionOutcome.NotAuthorized
                )
            }
            port.unmute(
                request.actor.toIntegration(),
                request.targetId,
            ).toCallResult()
        }

    override fun confine(
        request: ModerationConfinementRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle =
        submitAction(callback) {
            if (!gate(request.actor, CAPABILITY_CONFINE)) {
                return@submitAction ApiCallResult.Completed(
                    ModerationActionOutcome.NotAuthorized
                )
            }
            port.confine(
                request.actor.toIntegration(),
                request.targetId,
                request.reason,
            ).toCallResult()
        }

    override fun release(
        request: ModerationConfinementRequest,
        callback: ApiCallback<ModerationActionOutcome>,
    ): ApiRequestHandle =
        submitAction(callback) {
            if (!gate(request.actor, CAPABILITY_RELEASE)) {
                return@submitAction ApiCallResult.Completed(
                    ModerationActionOutcome.NotAuthorized
                )
            }
            port.release(
                request.actor.toIntegration(),
                request.targetId,
            ).toCallResult()
        }

    private fun submitAction(
        callback: ApiCallback<ModerationActionOutcome>,
        task: suspend () -> ApiCallResult<ModerationActionOutcome>,
    ): ApiRequestHandle =
        dispatcher.submit(
            task = task,
            deliver = callback::onComplete,
        )

    /**
     * A player actor is authorized by the port (online + capability). An addon actor is checked
     * against its live capability snapshot here; a free string actor can never reach the port.
     */
    private fun gate(actor: ModerationActionActor, capability: String): Boolean =
        when (actor) {
            is ModerationActionActor.Player -> true
            is ModerationActionActor.Addon -> actor.access.isAllowed(capability)
        }

}

/** Session-scoped fan-out of committed moderation facts to public listeners. */
private class ModerationEventFanOut : ModerationEvents {

    private val muted = CopyOnWriteArrayList<ApiEventListener<PlayerMuted>>()
    private val unmuted = CopyOnWriteArrayList<ApiEventListener<PlayerUnmuted>>()
    private val confined = CopyOnWriteArrayList<ApiEventListener<PlayerConfined>>()
    private val released = CopyOnWriteArrayList<ApiEventListener<PlayerReleased>>()
    private val vanish = CopyOnWriteArrayList<ApiEventListener<PlayerVanishStateChanged>>()

    override fun onMuted(listener: ApiEventListener<PlayerMuted>): Subscription =
        register(muted, listener)

    override fun onUnmuted(listener: ApiEventListener<PlayerUnmuted>): Subscription =
        register(unmuted, listener)

    override fun onConfined(listener: ApiEventListener<PlayerConfined>): Subscription =
        register(confined, listener)

    override fun onReleased(listener: ApiEventListener<PlayerReleased>): Subscription =
        register(released, listener)

    override fun onVanishStateChanged(
        listener: ApiEventListener<PlayerVanishStateChanged>,
    ): Subscription = register(vanish, listener)

    fun dispatch(event: ModerationDomainEvent) {
        when (event) {
            is ModerationDomainEvent.Muted ->
                muted.forEach {
                    fire(
                        it,
                        PlayerMuted(event.playerId, event.until, event.actionId)
                    )
                }

            is ModerationDomainEvent.Unmuted ->
                unmuted.forEach {
                    fire(
                        it,
                        PlayerUnmuted(event.playerId, event.actionId)
                    )
                }

            is ModerationDomainEvent.Confined ->
                confined.forEach {
                    fire(
                        it,
                        PlayerConfined(event.playerId, event.actionId)
                    )
                }

            is ModerationDomainEvent.Released ->
                released.forEach {
                    fire(
                        it,
                        PlayerReleased(event.playerId, event.actionId)
                    )
                }

            is ModerationDomainEvent.VanishStateChanged ->
                vanish.forEach {
                    fire(
                        it,
                        PlayerVanishStateChanged(event.playerId, event.vanished)
                    )
                }
        }
    }

    fun clear() {
        muted.clear()
        unmuted.clear()
        confined.clear()
        released.clear()
        vanish.clear()
    }

    private fun <T> register(
        listeners: CopyOnWriteArrayList<ApiEventListener<T>>,
        listener: ApiEventListener<T>,
    ): Subscription {
        listeners += listener
        return Subscription { listeners.remove(listener) }
    }

    private fun <T> fire(listener: ApiEventListener<T>, event: T) {
        runCatching { listener.onEvent(event) }
                .onFailure { LOGGER.warn("moderation_event_listener_failed", it) }
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ModerationEventFanOut::class.java)

    }

}

private fun ModerationActionActor.toIntegration(): ModerationIntegrationActor =
    when (this) {
        is ModerationActionActor.Player -> ModerationIntegrationActor.Player(playerId)
        is ModerationActionActor.Addon -> ModerationIntegrationActor.Addon(access.addonId)
    }

private fun ModerationIntegrationStatus.toPublic(): ModerationStatusOutcome =
    when (this) {
        is ModerationIntegrationStatus.Known -> ModerationStatusOutcome.Known(
            ModerationStatusView(
                muted = summary.muted,
                mutedUntil = summary.mutedUntil,
                confined = summary.confined,
            )
        )

        ModerationIntegrationStatus.NotFound ->
            ModerationStatusOutcome.NotFound

        ModerationIntegrationStatus.NotAuthorized ->
            ModerationStatusOutcome.NotAuthorized

        ModerationIntegrationStatus.StorageUnavailable ->
            ModerationStatusOutcome.StorageUnavailable

        // Handled by the caller before this mapping; kept for exhaustiveness.
        ModerationIntegrationStatus.RuntimeStopping ->
            ModerationStatusOutcome.StorageUnavailable
    }

private fun ModerationIntegrationOutcome.toCallResult(): ApiCallResult<ModerationActionOutcome> =
    when (this) {
        ModerationIntegrationOutcome.RuntimeStopping ->
            ApiCallResult.Stopping

        ModerationIntegrationOutcome.Unavailable ->
            ApiCallResult.Unavailable("administration.jail")

        else -> ApiCallResult.Completed(toPublic())
    }

private fun ModerationIntegrationOutcome.toPublic(): ModerationActionOutcome =
    when (this) {
        is ModerationIntegrationOutcome.Applied ->
            ModerationActionOutcome.Applied(actionId)

        ModerationIntegrationOutcome.AlreadyInState ->
            ModerationActionOutcome.AlreadyInState

        ModerationIntegrationOutcome.TargetUnknown ->
            ModerationActionOutcome.TargetUnknown

        ModerationIntegrationOutcome.ProtectedTarget ->
            ModerationActionOutcome.ProtectedTarget

        ModerationIntegrationOutcome.InvalidDuration ->
            ModerationActionOutcome.InvalidDuration

        ModerationIntegrationOutcome.InvalidReason ->
            ModerationActionOutcome.InvalidDuration

        ModerationIntegrationOutcome.NotAuthorized ->
            ModerationActionOutcome.NotAuthorized

        ModerationIntegrationOutcome.StorageUnavailable ->
            ModerationActionOutcome.StorageUnavailable

        ModerationIntegrationOutcome.Unavailable ->
            ModerationActionOutcome.StorageUnavailable

        ModerationIntegrationOutcome.RuntimeStopping ->
            ModerationActionOutcome.StorageUnavailable

        ModerationIntegrationOutcome.PartiallyApplied ->
            ModerationActionOutcome.PartiallyApplied
    }
