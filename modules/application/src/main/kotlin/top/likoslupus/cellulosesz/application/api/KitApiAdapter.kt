package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import top.likoslupus.cellulosesz.api.kit.*
import top.likoslupus.cellulosesz.api.kit.KitAvailabilityView
import top.likoslupus.cellulosesz.api.kit.KitReuseView
import top.likoslupus.cellulosesz.api.kit.KitSummary
import top.likoslupus.cellulosesz.utility.integration.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import top.likoslupus.cellulosesz.utility.integration.KitAvailabilityView as PortAvailability
import top.likoslupus.cellulosesz.utility.integration.KitReuseView as PortReuse
import top.likoslupus.cellulosesz.utility.integration.KitSummary as PortSummary

/**
 * The Kit domain adapter. Catalog reads, player claims and authorized system grants all route
 * through [KitIntegrationPort], so they reuse the exact command-path rules. The adapter owns the
 * session-scoped event fan-out; the composition root forwards committed catalog changes through
 * [dispatchCatalogChanged] and clears the sinks when the session stops.
 *
 * [grantByAddon] is the only system-authorization gate: it requires the requesting addon to hold the
 * `kit.grant` capability before any domain work happens.
 */
internal class KitApiAdapter(
    private val port: KitIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
) : KitsApi {

    private val sinks = KitEventSinks()
    override val events: KitEvents = sinks.events

    override fun listFor(
        actorId: UUID,
        callback: ApiCallback<KitListOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = { ApiCallResult.Completed(port.list(actorId).toApi()) },
        deliver = callback::onComplete,
    )

    override fun availability(
        actorId: UUID,
        kitName: String,
        callback: ApiCallback<KitAvailabilityOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.availability(
                    actorId,
                    kitName
                ).toApi()
            )
        },
        deliver = callback::onComplete,
    )

    override fun claimAsPlayer(
        actorId: UUID,
        kitName: String,
        callback: ApiCallback<KitClaimOutcome>,
    ): ApiRequestHandle = dispatcher.submit(
        task = {
            ApiCallResult.Completed(
                port.claim(
                    actorId,
                    kitName
                )
            )
        },
        deliver = { result ->
            when (result) {
                is ApiCallResult.Completed -> {
                    val claimed = result.value
                    if (claimed is KitClaimResult.Delivered) {
                        sinks.dispatchClaimed(
                            KitClaimed(
                                playerId = actorId,
                                kitName = claimed.kitName,
                                deliveryStatus = deliveryStatus(claimed.droppedStacks),
                            )
                        )
                    }
                    callback.onComplete(ApiCallResult.Completed(claimed.toApi()))
                }

                ApiCallResult.NotReady -> callback.onComplete(ApiCallResult.NotReady)
                ApiCallResult.Stopping -> callback.onComplete(ApiCallResult.Stopping)
                is ApiCallResult.Unavailable -> callback.onComplete(result)
                is ApiCallResult.Failed -> callback.onComplete(result)
            }
        },
    )

    override fun grantByAddon(
        request: AddonKitGrantRequest,
        callback: ApiCallback<KitGrantOutcome>,
    ): ApiRequestHandle {
        // System capability gate: no domain work happens without `kit.grant`.
        if (!request.access.isAllowed("kit.grant")) {
            return dispatcher.submit(
                task = { ApiCallResult.Completed(KitGrantOutcome.NotAuthorized) },
                deliver = callback::onComplete,
            )
        }

        val grant = KitGrantRequest(
            addonId = request.access.addonId,
            playerId = request.playerId,
            kitName = request.kitName,
            grantId = request.grantId,
        )
        return dispatcher.submit(
            task = { ApiCallResult.Completed(port.grant(grant)) },
            deliver = { result ->
                when (result) {
                    is ApiCallResult.Completed -> {
                        val granted = result.value
                        if (granted is KitGrantResult.Delivered) {
                            sinks.dispatchGranted(
                                KitGranted(
                                    playerId = request.playerId,
                                    addonId = grant.addonId,
                                    grantId = granted.grantId,
                                    kitName = granted.kitName,
                                    deliveryStatus = deliveryStatus(granted.droppedStacks),
                                )
                            )
                        }
                        callback.onComplete(granted.toApi())
                    }

                    ApiCallResult.NotReady -> callback.onComplete(ApiCallResult.NotReady)
                    ApiCallResult.Stopping -> callback.onComplete(ApiCallResult.Stopping)
                    is ApiCallResult.Unavailable -> callback.onComplete(result)
                    is ApiCallResult.Failed -> callback.onComplete(result)
                }
            },
        )
    }

    /** Forwards a committed catalog change to this session's listeners (server thread). */
    fun dispatchCatalogChanged(event: KitCatalogChangeEvent) =
        sinks.dispatchCatalogChanged(
            KitCatalogChanged(
                kitName = event.kitName,
                change = event.change.toApi(),
                occurredAt = event.occurredAt,
            )
        )

    /** Releases every kit listener; a closed adapter never dispatches again. */
    fun close() = sinks.close()

}

private class KitEventSinks {

    @Volatile private var closed = false

    private val claimed = CopyOnWriteArrayList<ApiEventListener<KitClaimed>>()
    private val granted = CopyOnWriteArrayList<ApiEventListener<KitGranted>>()
    private val catalog = CopyOnWriteArrayList<ApiEventListener<KitCatalogChanged>>()

    val events: KitEvents = object : KitEvents {

        override fun onClaimed(listener: ApiEventListener<KitClaimed>): Subscription =
            subscribe(claimed, listener)

        override fun onGranted(listener: ApiEventListener<KitGranted>): Subscription =
            subscribe(granted, listener)

        override fun onCatalogChanged(listener: ApiEventListener<KitCatalogChanged>): Subscription =
            subscribe(catalog, listener)

    }

    fun dispatchClaimed(event: KitClaimed) {
        if (closed) {
            return
        }
        claimed.forEach { runCatching { it.onEvent(event) } }
    }

    fun dispatchGranted(event: KitGranted) {
        if (closed) {
            return
        }
        granted.forEach { runCatching { it.onEvent(event) } }
    }

    fun dispatchCatalogChanged(event: KitCatalogChanged) {
        if (closed) {
            return
        }
        catalog.forEach { runCatching { it.onEvent(event) } }
    }

    fun close() {
        closed = true
        claimed.clear()
        granted.clear()
        catalog.clear()
    }

    private fun <T> subscribe(
        listeners: CopyOnWriteArrayList<ApiEventListener<T>>,
        listener: ApiEventListener<T>,
    ): Subscription {
        if (closed) {
            return Subscription { }
        }
        listeners += listener
        return Subscription { listeners.remove(listener) }
    }

}

private fun deliveryStatus(droppedStacks: Int): KitDeliveryStatus =
    when {
        droppedStacks > 0 -> KitDeliveryStatus.DELIVERED_WITH_OVERFLOW
        else -> KitDeliveryStatus.DELIVERED
    }

private fun KitListResult.toApi(): KitListOutcome = when (this) {
    is KitListResult.Listed -> KitListOutcome.Listed(kits.map { it.toApi() })
    KitListResult.Disabled -> KitListOutcome.Disabled
    KitListResult.Loading -> KitListOutcome.Loading
    KitListResult.StorageUnavailable -> KitListOutcome.StorageUnavailable
}

private fun KitAvailabilityResult.toApi(): KitAvailabilityOutcome = when (this) {
    is KitAvailabilityResult.Found -> KitAvailabilityOutcome.Found(availability.toApi())
    KitAvailabilityResult.NotFound -> KitAvailabilityOutcome.NotFound
    KitAvailabilityResult.Disabled -> KitAvailabilityOutcome.Disabled
    KitAvailabilityResult.Loading -> KitAvailabilityOutcome.Loading
    KitAvailabilityResult.StorageUnavailable -> KitAvailabilityOutcome.StorageUnavailable
}

private fun KitClaimResult.toApi(): KitClaimOutcome = when (this) {
    is KitClaimResult.Delivered ->
        KitClaimOutcome.Delivered(
            droppedStacks,
            claimRecordedFully,
            equippedStacks
        )

    KitClaimResult.Disabled -> KitClaimOutcome.Disabled
    KitClaimResult.Loading -> KitClaimOutcome.Loading
    KitClaimResult.NotFound -> KitClaimOutcome.NotFound
    KitClaimResult.InvalidName -> KitClaimOutcome.InvalidName
    is KitClaimResult.Cooldown -> KitClaimOutcome.Cooldown(remaining)
    KitClaimResult.AlreadyUsed -> KitClaimOutcome.AlreadyUsed
    KitClaimResult.InventoryFull -> KitClaimOutcome.InventoryFull
    KitClaimResult.PlayerOffline -> KitClaimOutcome.PlayerOffline
    KitClaimResult.StorageUnavailable -> KitClaimOutcome.StorageUnavailable
    KitClaimResult.DeliveryFailedAfterReservation -> KitClaimOutcome.DeliveryFailedAfterReservation
    is KitClaimResult.Denied -> KitClaimOutcome.Denied(reasonKey)
}

private fun KitGrantResult.toApi(): ApiCallResult<KitGrantOutcome> = when (this) {
    is KitGrantResult.Delivered ->
        ApiCallResult.Completed(
            KitGrantOutcome.Delivered(
                grantId,
                droppedStacks,
                equippedStacks
            )
        )

    KitGrantResult.AlreadyProcessed ->
        ApiCallResult.Completed(KitGrantOutcome.AlreadyProcessed)

    KitGrantResult.PreviouslyUncertain ->
        ApiCallResult.Completed(KitGrantOutcome.PreviouslyUncertain)

    KitGrantResult.PlayerOffline ->
        ApiCallResult.Completed(KitGrantOutcome.PlayerOffline)

    KitGrantResult.KitNotFound ->
        ApiCallResult.Completed(KitGrantOutcome.KitNotFound)

    KitGrantResult.InventoryFull ->
        ApiCallResult.Completed(KitGrantOutcome.InventoryFull)

    KitGrantResult.DeliveryFailed ->
        ApiCallResult.Completed(KitGrantOutcome.DeliveryFailed)

    KitGrantResult.StorageUnavailable ->
        ApiCallResult.Completed(KitGrantOutcome.StorageUnavailable)

    KitGrantResult.Disabled -> ApiCallResult.Unavailable("kits")
    KitGrantResult.Loading -> ApiCallResult.Unavailable("kits")
}

private fun KitCatalogChangeKind.toApi(): KitCatalogChange = when (this) {
    KitCatalogChangeKind.CREATED -> KitCatalogChange.CREATED
    KitCatalogChangeKind.UPDATED -> KitCatalogChange.UPDATED
    KitCatalogChangeKind.DELETED -> KitCatalogChange.DELETED
}

private fun PortReuse.toApi(): KitReuseView = when (this) {
    PortReuse.Always -> KitReuseView.Always
    PortReuse.Once -> KitReuseView.Once
    is PortReuse.Cooldown -> KitReuseView.Cooldown(duration)
}

private fun PortAvailability.toApi(): KitAvailabilityView = when (this) {
    PortAvailability.Available -> KitAvailabilityView.Available
    PortAvailability.AlreadyUsed -> KitAvailabilityView.AlreadyUsed
    is PortAvailability.Cooldown -> KitAvailabilityView.Cooldown(remaining)
}

private fun PortSummary.toApi(): KitSummary =
    KitSummary(
        name = name,
        reuse = reuse.toApi(),
        availability = availability.toApi()
    )
