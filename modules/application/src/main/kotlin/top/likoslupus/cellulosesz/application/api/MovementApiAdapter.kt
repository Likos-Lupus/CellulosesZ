package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.TeleportCommitted
import top.likoslupus.cellulosesz.api.movement.*
import top.likoslupus.cellulosesz.movement.integration.*

/**
 * The Movement domain adapter. It maps the movement-local integration results onto the public sealed
 * outcomes and fans committed facts out to this session's listeners. The composition root forwards a
 * fresh adapter per server session through the `dispatch*` methods and drops it with [clear].
 */
internal class MovementApiAdapter(
    private val port: MovementIntegrationPort,
    private val dispatcher: ApiRequestDispatcher,
    worlds: WorldsApi,
) : MovementApi {

    private val sinks = MovementEventSinks()

    override val homes: HomesApi = MovementHomesAdapter(port, dispatcher)

    override val warps: WarpsApi = MovementWarpsAdapter(port, dispatcher)

    override val spawn: SpawnApi = MovementSpawnAdapter(port, dispatcher)

    override val teleport: TeleportApi = MovementTeleportAdapter(port, dispatcher)

    override val requests: TeleportRequestApi = MovementRequestsAdapter(port, dispatcher)

    override val worlds: WorldsApi = worlds

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
