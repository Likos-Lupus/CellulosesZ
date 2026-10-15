package top.likoslupus.cellulosesz.application.api

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.*
import top.likoslupus.cellulosesz.movement.CommittedTeleport
import top.likoslupus.cellulosesz.movement.TeleportOrigin
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/** The concrete API view handed to consumers; every field is an adapter over the live features. */
internal class ApplicationApi(
    override val version: ApiVersion,
    override val players: PlayerLookup,
    override val visibility: VisibilityLookup,
    override val teleports: TeleportEvents,
) : CellulosesZApi

/**
 * The provider a loader-native shim publishes. [attach] must be called once, right after
 * construction, before [markReady]. Availability is a cheap snapshot; [whenReady] invokes a listener
 * as soon as the API is ready (immediately if it already is).
 */
internal class ApplicationApiProvider : CellulosesZApiProvider {

    @Volatile private var api: CellulosesZApi? = null
    @Volatile private var state: ApiAvailability = ApiAvailability.NOT_READY

    private val pending: ConcurrentLinkedQueue<ApiReadyListener> = ConcurrentLinkedQueue()

    fun attach(value: CellulosesZApi) {
        api = value
    }

    override fun availability(): ApiAvailability =
        state

    override fun whenReady(listener: ApiReadyListener): Subscription {
        val current = api
        if (state == ApiAvailability.READY && current != null) {
            listener.onReady(current)
            return Subscription { }
        }
        pending += listener
        return Subscription { pending.remove(listener) }
    }

    fun markReady() {
        val current = api ?: error("API provider was marked ready before an API was attached")
        state = ApiAvailability.READY
        while (true) {
            val listener = pending.poll() ?: break
            runCatching { listener.onReady(current) }
                    .onFailure { LOGGER.warn("api_ready_listener_failed", it) }
        }
    }

    fun markStopping() {
        state = ApiAvailability.STOPPING
    }

    fun markStopped() {
        state = ApiAvailability.STOPPED
        pending.clear()
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ApplicationApiProvider::class.java)

    }

}

/** Read-only player identity, gated on the session being ready. */
internal class ApiPlayerLookup(
    private val availability: () -> ApiAvailability,
    private val resolve: (UUID) -> PlayerIdentity?,
) : PlayerLookup {

    override fun findById(id: UUID): ApiQuery<PlayerIdentity?> =
        when (availability()) {
            ApiAvailability.READY -> ApiQuery.Available(resolve(id))
            ApiAvailability.NOT_READY -> ApiQuery.NotReady
            ApiAvailability.STOPPING,
            ApiAvailability.STOPPED -> ApiQuery.Stopping
        }

}

/** Read-only visibility, gated on the session being ready. */
internal class ApiVisibilityLookup(
    private val availability: () -> ApiAvailability,
    private val canSeeDelegate: (viewerId: UUID, targetId: UUID) -> Boolean,
) : VisibilityLookup {

    override fun canSee(viewerId: UUID, targetId: UUID): ApiQuery<Boolean> =
        when (availability()) {
            ApiAvailability.READY -> ApiQuery.Available(canSeeDelegate(viewerId, targetId))
            ApiAvailability.NOT_READY -> ApiQuery.NotReady
            ApiAvailability.STOPPING,
            ApiAvailability.STOPPED -> ApiQuery.Stopping
        }

}

/** Fan-out of confirmed teleports to consumer listeners; one bad listener never blocks the rest. */
internal class ApiTeleportEvents : TeleportEvents {

    private val listeners: CopyOnWriteArrayList<TeleportListener> = CopyOnWriteArrayList()

    override fun subscribe(listener: TeleportListener): Subscription {
        listeners += listener
        return Subscription { listeners.remove(listener) }
    }

    fun dispatch(event: TeleportCommitted) {
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
