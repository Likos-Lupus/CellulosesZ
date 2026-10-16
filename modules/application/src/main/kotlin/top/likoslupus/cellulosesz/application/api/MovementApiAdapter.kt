package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.TeleportCommitted
import top.likoslupus.cellulosesz.api.actor.ApiActor
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.toJavaDuration

/**
 * The Movement domain adapter. It maps the movement-local integration results onto the public sealed
 * outcomes and fans committed facts out to this session's listeners. The composition root forwards a
 * fresh adapter per server session through the `dispatch*` methods and drops it with [clear].
 */
internal class MovementApiAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : MovementApi {

    private val sinks = MovementEventSinks()

    override val homes: HomesApi = MovementHomesAdapter(port, dispatcher)

    override val warps: WarpsApi = MovementWarpsAdapter(port, dispatcher)

    override val spawn: SpawnApi = MovementSpawnAdapter(port, dispatcher)

    override val teleport: TeleportApi = MovementTeleportAdapter(port, dispatcher)

    override val requests: TeleportRequestApi = MovementRequestsAdapter(port, dispatcher)

    override val events: MovementEvents = sinks.events

    fun dispatchTeleportCommitted(event: TeleportCommitted) =
        sinks.dispatchTeleportCommitted(event)

    fun dispatchHomeChanged(event: HomeChanged) =
        sinks.dispatchHomeChanged(event)

    fun dispatchWarpChanged(event: WarpChanged) =
        sinks.dispatchWarpChanged(event)

    fun dispatchSpawnChanged(event: SpawnChanged) =
        sinks.dispatchSpawnChanged(event)

    fun dispatchTeleportRequestChanged(event: TeleportRequestChanged) =
        sinks.dispatchTeleportRequestChanged(event)

    fun clear() = sinks.clear()

}

private class MovementHomesAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : HomesApi {

    override fun listOwned(
        ownerId: UUID,
        callback: ApiCallback<HomeListOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.listHomes(
                    ownerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun findOwned(
        ownerId: UUID,
        name: String?,
        callback: ApiCallback<HomeLookupOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.findHome(
                    ownerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun setAtCurrentPosition(
        actorId: UUID,
        name: String?,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.setHomeAtCurrentPosition(
                    actorId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun renameOwn(
        actorId: UUID,
        oldName: String,
        newName: String,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.renameHome(
                    actorId,
                    oldName,
                    newName
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun deleteOwn(
        actorId: UUID,
        name: String,
        callback: ApiCallback<HomeMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.deleteHome(
                    actorId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}

private class MovementWarpsAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : WarpsApi {

    override fun listVisible(
        viewerId: UUID,
        callback: ApiCallback<WarpListOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.listWarps(
                    viewerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun findVisible(
        viewerId: UUID,
        name: String,
        callback: ApiCallback<WarpLookupOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.findWarp(
                    viewerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun create(
        actor: ApiActor.Player,
        name: String,
        callback: ApiCallback<WarpMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.createWarp(
                    actor.playerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun delete(
        actor: ApiActor.Player,
        name: String,
        callback: ApiCallback<WarpMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.deleteWarp(
                    actor.playerId,
                    name
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}

private class MovementSpawnAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : SpawnApi {

    override fun configured(
        viewerId: UUID,
        callback: ApiCallback<SpawnQueryOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.configuredSpawn(
                    viewerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun travelToSpawn(
        playerId: UUID,
        callback: ApiCallback<TeleportActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.travel(
                    playerId,
                    MovementTravelTarget.Spawn
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun setFromCurrent(
        actor: ApiActor.Player,
        callback: ApiCallback<SpawnMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.setSpawnFromCurrent(
                    actor.playerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun clear(
        actor: ApiActor.Player,
        callback: ApiCallback<SpawnMutationOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.clearSpawn(
                    actor.playerId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}

private class MovementTeleportAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : TeleportApi {

    override fun travel(
        request: PlayerTeleportRequest,
        callback: ApiCallback<TeleportActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.travel(
                    request.playerId,
                    request.target.toMovementTarget()
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}

private class MovementRequestsAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : TeleportRequestApi {

    override fun send(
        playerId: UUID,
        targetId: UUID,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.sendTeleportRequest(
                    playerId,
                    targetId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun accept(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.acceptTeleportRequest(
                    playerId,
                    requestId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun deny(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.denyTeleportRequest(
                    playerId,
                    requestId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun cancelOwn(
        playerId: UUID,
        requestId: String,
        callback: ApiCallback<TeleportRequestActionOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.cancelOwnTeleportRequest(
                    playerId,
                    requestId
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

}

private class MovementEventSinks {

    private val teleportCommitted = CopyOnWriteArrayList<ApiEventListener<TeleportCommitted>>()
    private val homeChanged = CopyOnWriteArrayList<ApiEventListener<HomeChanged>>()
    private val warpChanged = CopyOnWriteArrayList<ApiEventListener<WarpChanged>>()
    private val spawnChanged = CopyOnWriteArrayList<ApiEventListener<SpawnChanged>>()
    private val requestChanged = CopyOnWriteArrayList<ApiEventListener<TeleportRequestChanged>>()

    val events: MovementEvents = object : MovementEvents {

        override fun onTeleportCommitted(
            listener: ApiEventListener<TeleportCommitted>,
        ): Subscription =
            teleportCommitted.subscribeSink(listener)

        override fun onHomeChanged(
            listener: ApiEventListener<HomeChanged>,
        ): Subscription =
            homeChanged.subscribeSink(listener)

        override fun onWarpChanged(
            listener: ApiEventListener<WarpChanged>,
        ): Subscription =
            warpChanged.subscribeSink(listener)

        override fun onSpawnChanged(
            listener: ApiEventListener<SpawnChanged>,
        ): Subscription =
            spawnChanged.subscribeSink(listener)

        override fun onTeleportRequestChanged(
            listener: ApiEventListener<TeleportRequestChanged>,
        ): Subscription =
            requestChanged.subscribeSink(listener)

    }

    fun dispatchTeleportCommitted(event: TeleportCommitted) =
        teleportCommitted.forEach { runCatching { it.onEvent(event) } }

    fun dispatchHomeChanged(event: HomeChanged) =
        homeChanged.forEach { runCatching { it.onEvent(event) } }

    fun dispatchWarpChanged(event: WarpChanged) =
        warpChanged.forEach { runCatching { it.onEvent(event) } }

    fun dispatchSpawnChanged(event: SpawnChanged) =
        spawnChanged.forEach { runCatching { it.onEvent(event) } }

    fun dispatchTeleportRequestChanged(event: TeleportRequestChanged) =
        requestChanged.forEach { runCatching { it.onEvent(event) } }

    fun clear() {
        teleportCommitted.clear()
        homeChanged.clear()
        warpChanged.clear()
        spawnChanged.clear()
        requestChanged.clear()
    }

}

private fun <T> CopyOnWriteArrayList<ApiEventListener<T>>.subscribeSink(
    listener: ApiEventListener<T>,
): Subscription {
    add(listener)
    return Subscription { remove(listener) }
}

private fun TeleportTarget.toMovementTarget(): MovementTravelTarget =
    when (this) {
        is TeleportTarget.Home -> MovementTravelTarget.Home(name)
        is TeleportTarget.Warp -> MovementTravelTarget.Warp(name)
        TeleportTarget.Spawn -> MovementTravelTarget.Spawn
        is TeleportTarget.Player -> MovementTravelTarget.Player(playerId)
    }

private fun MovementPosition.toApi(): ApiPosition =
    ApiPosition(dimension, x, y, z, yaw, pitch)

private fun MovementHome.toView(): HomeView =
    HomeView(ownerId, name, position.toApi())

private fun MovementWarp.toView(): WarpView =
    WarpView(name, position?.toApi(), createdBy, updatedAt)

private fun MovementHomeListResult.toApi(): HomeListOutcome =
    when (this) {
        is MovementHomeListResult.Listed -> HomeListOutcome.Listed(homes.map { it.toView() })
        MovementHomeListResult.StorageUnavailable -> HomeListOutcome.StorageUnavailable
    }

private fun MovementHomeLookupResult.toApi(): HomeLookupOutcome =
    when (this) {
        is MovementHomeLookupResult.Found -> HomeLookupOutcome.Found(home.toView())
        MovementHomeLookupResult.NotFound -> HomeLookupOutcome.NotFound
        MovementHomeLookupResult.InvalidName -> HomeLookupOutcome.InvalidName
        MovementHomeLookupResult.StorageUnavailable -> HomeLookupOutcome.StorageUnavailable
    }

private fun MovementHomeMutationResult.toApi(): HomeMutationOutcome =
    when (this) {
        is MovementHomeMutationResult.Created -> HomeMutationOutcome.Created(name)
        is MovementHomeMutationResult.Updated -> HomeMutationOutcome.Updated(name)
        is MovementHomeMutationResult.Renamed -> HomeMutationOutcome.Renamed(name)
        is MovementHomeMutationResult.Deleted -> HomeMutationOutcome.Deleted(name)
        MovementHomeMutationResult.InvalidName -> HomeMutationOutcome.InvalidName
        MovementHomeMutationResult.NotFound -> HomeMutationOutcome.NotFound
        MovementHomeMutationResult.AlreadyExists -> HomeMutationOutcome.AlreadyExists
        MovementHomeMutationResult.LimitReached -> HomeMutationOutcome.LimitReached
        MovementHomeMutationResult.PlayerOffline -> HomeMutationOutcome.PlayerOffline
        MovementHomeMutationResult.StorageUnavailable -> HomeMutationOutcome.StorageUnavailable
    }

private fun MovementWarpListResult.toApi(): WarpListOutcome =
    when (this) {
        is MovementWarpListResult.Listed -> WarpListOutcome.Listed(warps.map { it.toView() })
        MovementWarpListResult.NotAuthorized -> WarpListOutcome.NotAuthorized
        MovementWarpListResult.StorageUnavailable -> WarpListOutcome.StorageUnavailable
    }

private fun MovementWarpLookupResult.toApi(): WarpLookupOutcome =
    when (this) {
        is MovementWarpLookupResult.Found -> WarpLookupOutcome.Found(warp.toView())
        MovementWarpLookupResult.NotFound -> WarpLookupOutcome.NotFound
        MovementWarpLookupResult.InvalidName -> WarpLookupOutcome.InvalidName
        MovementWarpLookupResult.NotAuthorized -> WarpLookupOutcome.NotAuthorized
        MovementWarpLookupResult.StorageUnavailable -> WarpLookupOutcome.StorageUnavailable
    }

private fun MovementWarpMutationResult.toApi(): WarpMutationOutcome =
    when (this) {
        is MovementWarpMutationResult.Created -> WarpMutationOutcome.Created(name)
        is MovementWarpMutationResult.Deleted -> WarpMutationOutcome.Deleted(name)
        MovementWarpMutationResult.NotFound -> WarpMutationOutcome.NotFound
        MovementWarpMutationResult.InvalidName -> WarpMutationOutcome.InvalidName
        MovementWarpMutationResult.NotAuthorized -> WarpMutationOutcome.NotAuthorized
        MovementWarpMutationResult.PlayerOffline -> WarpMutationOutcome.PlayerOffline
        MovementWarpMutationResult.StorageUnavailable -> WarpMutationOutcome.StorageUnavailable
    }

private fun MovementSpawnQueryResult.toApi(): SpawnQueryOutcome =
    when (this) {
        is MovementSpawnQueryResult.Configured -> SpawnQueryOutcome.Configured(position.toApi())
        MovementSpawnQueryResult.NotConfigured -> SpawnQueryOutcome.NotConfigured
        MovementSpawnQueryResult.NotAuthorized -> SpawnQueryOutcome.NotAuthorized
        MovementSpawnQueryResult.StorageUnavailable -> SpawnQueryOutcome.StorageUnavailable
    }

private fun MovementSpawnMutationResult.toApi(): SpawnMutationOutcome =
    when (this) {
        MovementSpawnMutationResult.Updated -> SpawnMutationOutcome.Updated
        MovementSpawnMutationResult.Cleared -> SpawnMutationOutcome.Cleared
        MovementSpawnMutationResult.NotAuthorized -> SpawnMutationOutcome.NotAuthorized
        MovementSpawnMutationResult.PlayerOffline -> SpawnMutationOutcome.PlayerOffline
        MovementSpawnMutationResult.StorageUnavailable -> SpawnMutationOutcome.StorageUnavailable
    }

private fun MovementTravelResult.toApi(): TeleportActionOutcome =
    when (this) {
        is MovementTravelResult.Committed -> TeleportActionOutcome.Committed(
            TeleportReceiptView(
                playerId = receipt.playerId,
                from = receipt.from.toApi(),
                to = receipt.to.toApi(),
                origin = receipt.origin.toApiKind(),
            )
        )

        MovementTravelResult.PlayerOffline -> TeleportActionOutcome.PlayerOffline
        MovementTravelResult.TargetOffline -> TeleportActionOutcome.TargetOffline
        MovementTravelResult.DestinationNotFound -> TeleportActionOutcome.DestinationNotFound
        MovementTravelResult.Denied -> TeleportActionOutcome.Denied
        MovementTravelResult.Confined -> TeleportActionOutcome.Confined
        MovementTravelResult.UnsafeDestination -> TeleportActionOutcome.UnsafeDestination
        MovementTravelResult.PlatformRejected -> TeleportActionOutcome.PlatformRejected
        is MovementTravelResult.Cooldown ->
            TeleportActionOutcome.Cooldown(remaining.toJavaDuration())

        MovementTravelResult.AlreadyPending -> TeleportActionOutcome.AlreadyPending
        MovementTravelResult.Cancelled -> TeleportActionOutcome.Cancelled
        MovementTravelResult.OtherFailure -> TeleportActionOutcome.OtherFailure
    }

private fun MovementRequestActionResult.toApi(): TeleportRequestActionOutcome =
    when (this) {
        is MovementRequestActionResult.Created -> TeleportRequestActionOutcome.Created(requestId)
        MovementRequestActionResult.Accepted -> TeleportRequestActionOutcome.Accepted
        MovementRequestActionResult.Denied -> TeleportRequestActionOutcome.Denied
        MovementRequestActionResult.Cancelled -> TeleportRequestActionOutcome.Cancelled
        MovementRequestActionResult.Expired -> TeleportRequestActionOutcome.Expired
        MovementRequestActionResult.TargetUnavailable ->
            TeleportRequestActionOutcome.TargetUnavailable

        MovementRequestActionResult.AlreadyPending -> TeleportRequestActionOutcome.AlreadyPending
        MovementRequestActionResult.NotAuthorized -> TeleportRequestActionOutcome.NotAuthorized
        MovementRequestActionResult.RequestLimit -> TeleportRequestActionOutcome.RequestLimit
        MovementRequestActionResult.OtherFailure -> TeleportRequestActionOutcome.OtherFailure
    }
