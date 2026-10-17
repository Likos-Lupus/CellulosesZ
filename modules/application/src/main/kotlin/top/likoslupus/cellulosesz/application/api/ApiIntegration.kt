package top.likoslupus.cellulosesz.application.api

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.*
import top.likoslupus.cellulosesz.api.communication.CommunicationApi
import top.likoslupus.cellulosesz.api.economy.EconomyApi
import top.likoslupus.cellulosesz.api.extension.ExtensionRegistrationListener
import top.likoslupus.cellulosesz.api.extension.ExtensionsApi
import top.likoslupus.cellulosesz.api.kit.KitsApi
import top.likoslupus.cellulosesz.api.moderation.ModerationApi
import top.likoslupus.cellulosesz.api.movement.MovementApi
import top.likoslupus.cellulosesz.api.player.PlayersApi
import top.likoslupus.cellulosesz.api.presentation.PresentationApi
import top.likoslupus.cellulosesz.application.api.extension.ApplicationExtensionHub
import top.likoslupus.cellulosesz.movement.CommittedTeleport
import top.likoslupus.cellulosesz.movement.TeleportOrigin
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/** The concrete API view handed to consumers; every field is an adapter over the live features. */
internal class ApplicationApi(
    override val version: ApiVersion,
    override val players: PlayerLookup,
    override val visibility: VisibilityLookup,
    override val teleports: TeleportEvents,
    override val player: PlayersApi,
    override val movement: MovementApi,
    override val communication: CommunicationApi,
    override val kits: KitsApi,
    override val moderation: ModerationApi,
    override val economy: EconomyApi,
    override val presentation: PresentationApi,
    override val extensions: ExtensionsApi,
) : CellulosesZApi

/**
 * The monotonic phase of one Minecraft server session's API view. `CREATED -> READY -> CLOSED` never
 * moves backwards, so an old session's API view can never become available again when a new session
 * starts in the same JVM (integrated server).
 */
internal enum class SessionPhase {

    CREATED,
    READY,
    CLOSED

}

/** The one-shot lifecycle of a single API session, shared by its views and its teleport event sink. */
internal class SessionLease {

    @Volatile private var phase: SessionPhase = SessionPhase.CREATED

    fun phase(): SessionPhase =
        phase

    val isReady: Boolean get() = phase == SessionPhase.READY

    fun ready() {
        if (phase == SessionPhase.CREATED) phase = SessionPhase.READY
    }

    fun close() {
        phase = SessionPhase.CLOSED
    }

}

/**
 * The per-session materials the composition root wires together: the lease its view queries check and
 * the teleport event sink its listeners subscribe to. A new session gets fresh instances.
 */
internal class ApiSession(
    val epoch: Long,
    val lease: SessionLease,
    val teleports: ApiTeleportEvents,
)

/**
 * The provider a loader-native shim publishes. [beginSession] opens a new session, [attach] supplies
 * its view and [markReady]/[markStopping]/[markStopped] advance it. All state/queue operations are
 * linearized under one lock; user callbacks are never invoked while holding that lock.
 *
 * A listener registered with [whenReady] before READY is delivered exactly once for that session
 * unless it is cancelled first. A view obtained from an earlier session returns [ApiQuery.Stopping]
 * forever once that session is closed; it is never revived by a later session.
 */
internal class ApplicationApiProvider(
    private val extensions: ApplicationExtensionHub,
) : CellulosesZApiProvider {

    private val lock = Any()
    private var state: ApiAvailability = ApiAvailability.NOT_READY
    private var epoch: Long = 0
    private var session: ApiSession? = null
    private var api: CellulosesZApi? = null
    private val pending = LinkedHashMap<Long, PendingListener>()
    private var nextRegistration: Long = 0
    private var registrationOpen: Boolean = false
    private val registrationPending = LinkedHashMap<Long, ExtensionRegistrationListener>()

    private class PendingListener(
        val id: Long,
        val epoch: Long,
        val listener: ApiReadyListener,
        val cancelled: AtomicBoolean,
    )

    /** Opens a fresh session and returns its lease + event sink. Called once per server start. */
    fun beginSession(): ApiSession = synchronized(lock) {
        val lease = SessionLease()
        val opened = ApiSession(++epoch, lease, ApiTeleportEvents(lease))
        session = opened
        api = null
        state = ApiAvailability.NOT_READY
        opened
    }

    /** Supplies the current session's API view. Must be called after [beginSession]. */
    fun attach(value: CellulosesZApi) = synchronized(lock) {
        check(session != null) { "attach called before beginSession" }
        api = value
    }

    /** Routes a confirmed teleport to the current session's listeners (a no-op outside a ready one). */
    fun dispatchTeleport(event: TeleportCommitted) {
        val current = synchronized(lock) { session }
        current?.teleports?.dispatch(event)
    }

    override fun availability(): ApiAvailability =
        synchronized(lock) { state }

    override fun whenReady(listener: ApiReadyListener): Subscription {
        val deliverNow: Pair<CellulosesZApi, ApiReadyListener>?
        val registration: PendingListener?
        synchronized(lock) {
            val current = session
            val currentApi = api
            when {
                state == ApiAvailability.READY
                        && current != null
                        && currentApi != null
                        && current.lease.isReady -> {
                    deliverNow = currentApi to listener
                    registration = null
                }

                else -> {
                    // No live ready session yet (never started, between sessions, or shutting down):
                    // bind to the next session's READY so an early registration is not lost. A session
                    // that is already shutting down is not the target — the next epoch is.
                    val targetEpoch = if (current != null
                        && state == ApiAvailability.NOT_READY
                    ) {
                        current.epoch
                    } else {
                        epoch + 1
                    }
                    val record = PendingListener(
                        ++nextRegistration,
                        targetEpoch,
                        listener,
                        AtomicBoolean(false),
                    )
                    pending[record.id] = record
                    deliverNow = null
                    registration = record
                }
            }
        }

        // The lock is released before user code runs, so a listener can never re-enter the provider
        // while it holds the lock.
        deliverNow?.let { (value, target) -> deliver(target, value) }

        return when (registration) {
            null -> Subscription { }
            else -> Subscription { cancel(registration.id) }
        }
    }

    /**
     * Opens the startup contribution window. Called once during mod init; contributions registered
     * before it opens are delivered immediately, and any queued listener is drained outside the lock.
     */
    fun openRegistrationWindow() {
        val queued: List<ExtensionRegistrationListener>
        synchronized(lock) {
            if (registrationOpen) {
                return
            }
            registrationOpen = true
            queued = registrationPending.values.toList()
            registrationPending.clear()
        }
        queued.forEach(::deliverRegistration)
    }

    override fun whenRegistrationOpen(
        listener: ExtensionRegistrationListener,
    ): Subscription {
        var id: Long? = null
        var deliverNow = false
        synchronized(lock) {
            if (registrationOpen) {
                deliverNow = true
            } else {
                val key = ++nextRegistration
                registrationPending[key] = listener
                id = key
            }
        }
        if (deliverNow) {
            deliverRegistration(listener)
        }

        return when (val key = id) {
            null -> Subscription { }
            else ->
                Subscription {
                    synchronized(lock) { registrationPending.remove(key) }
                }
        }
    }

    fun markReady() {
        val deliveries = mutableListOf<Pair<ApiReadyListener, CellulosesZApi>>()
        synchronized(lock) {
            val current = session ?: return
            val currentApi = api ?: return
            current.lease.ready()
            state = ApiAvailability.READY
            val iterator = pending.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.epoch == current.epoch) {
                    iterator.remove()
                    if (!entry.value.cancelled.get()) {
                        deliveries += entry.value.listener to currentApi
                    }
                }
            }
        }
        deliveries.forEach { (target, value) ->
            deliver(target, value)
        }
    }

    fun markStopping() {
        synchronized(lock) {
            val current = session ?: return
            current.lease.close()
            state = ApiAvailability.STOPPING
            dropPendingFor(current.epoch)
        }
    }

    fun markStopped() {
        synchronized(lock) {
            session?.lease?.close()
            session = null
            api = null
            state = ApiAvailability.STOPPED
            // Future-bound registrations (epoch + 1) survive into the next session; this session's
            // pending were already dropped in markStopping.
        }
    }

    private fun cancel(registrationId: Long) {
        synchronized(lock) {
            val record = pending[registrationId] ?: return
            record.cancelled.set(true)
            pending.remove(registrationId)
        }
    }

    private fun dropPendingFor(epoch: Long) {
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.epoch == epoch) {
                iterator.remove()
            }
        }
    }

    private fun deliver(listener: ApiReadyListener, value: CellulosesZApi) {
        runCatching { listener.onReady(value) }
                .onFailure { LOGGER.warn("api_ready_listener_failed", it) }
    }

    private fun deliverRegistration(listener: ExtensionRegistrationListener) {
        runCatching { listener.onRegistrationOpen(extensions) }
                .onFailure { LOGGER.warn("api_registration_listener_failed", it) }
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ApplicationApiProvider::class.java)

    }

}

/** Read-only player identity, bound to one session lease. */
internal class ApiPlayerLookup(
    private val lease: SessionLease,
    private val resolve: (UUID) -> PlayerIdentity?,
) : PlayerLookup {

    override fun findById(id: UUID): ApiQuery<PlayerIdentity?> =
        when (lease.phase()) {
            SessionPhase.READY -> ApiQuery.Available(resolve(id))
            SessionPhase.CREATED -> ApiQuery.NotReady
            SessionPhase.CLOSED -> ApiQuery.Stopping
        }

}

/** Read-only visibility, bound to one session lease. */
internal class ApiVisibilityLookup(
    private val lease: SessionLease,
    private val canSeeDelegate: (viewerId: UUID, targetId: UUID) -> Boolean,
) : VisibilityLookup {

    override fun canSee(viewerId: UUID, targetId: UUID): ApiQuery<Boolean> =
        when (lease.phase()) {
            SessionPhase.READY -> ApiQuery.Available(
                canSeeDelegate(viewerId, targetId)
            )

            SessionPhase.CREATED -> ApiQuery.NotReady

            SessionPhase.CLOSED -> ApiQuery.Stopping
        }

}

/**
 * Fan-out of confirmed teleports to this session's listeners. Only dispatches while the session is
 * ready; a bad listener never blocks the rest, and a subscriber added after the session closed gets
 * an inert subscription.
 */
internal class ApiTeleportEvents(
    private val lease: SessionLease,
) : TeleportEvents {

    private val listeners: CopyOnWriteArrayList<TeleportListener> = CopyOnWriteArrayList()

    override fun subscribe(listener: TeleportListener): Subscription {
        if (lease.phase() == SessionPhase.CLOSED) {
            return Subscription { }
        }

        listeners += listener
        return Subscription { listeners.remove(listener) }
    }

    fun dispatch(event: TeleportCommitted) {
        if (!lease.isReady) {
            return
        }

        listeners.forEach { listener ->
            runCatching { listener.onCommitted(event) }
                    .onFailure {
                        LOGGER.warn(
                            "api_teleport_listener_failed subject={}",
                            event.subjectId,
                            it
                        )
                    }
        }
    }

    fun clear() {
        listeners.clear()
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ApiTeleportEvents::class.java)

    }

}

/** Projects a committed teleport onto the public, Minecraft-free event. */
internal fun CommittedTeleport.toApiEvent(): TeleportCommitted =
    TeleportCommitted(
        subjectId = subjectId,
        kind = origin.toApiKind(),
        from = ApiPosition(
            from.dimension,
            from.x, from.y, from.z,
            from.yaw, from.pitch
        ),
        to = ApiPosition(
            to.dimension,
            to.x, to.y, to.z,
            to.yaw, to.pitch
        ),
    )

/** Stable mapping from the internal origin to the public kind; new causes fall back to [OTHER]. */
internal fun TeleportOrigin.toApiKind(): TeleportOriginKind =
    when (this) {
        TeleportOrigin.HOME -> TeleportOriginKind.HOME
        TeleportOrigin.WARP -> TeleportOriginKind.WARP
        TeleportOrigin.SPAWN -> TeleportOriginKind.SPAWN
        TeleportOrigin.BACK -> TeleportOriginKind.OTHER
        TeleportOrigin.REQUEST -> TeleportOriginKind.REQUEST
        TeleportOrigin.DIRECT -> TeleportOriginKind.DIRECT
        TeleportOrigin.ADMIN -> TeleportOriginKind.ADMIN
        TeleportOrigin.JAIL -> TeleportOriginKind.JAIL
        TeleportOrigin.RANDOM -> TeleportOriginKind.RANDOM
        TeleportOrigin.TERRAIN -> TeleportOriginKind.TERRAIN
        TeleportOrigin.OTHER -> TeleportOriginKind.OTHER
    }
