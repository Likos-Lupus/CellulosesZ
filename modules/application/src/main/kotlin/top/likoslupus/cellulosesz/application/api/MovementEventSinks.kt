package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.TeleportCommitted
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import top.likoslupus.cellulosesz.api.movement.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The per-session listener sinks behind [MovementEvents]. A dispatched event is isolated per
 * listener, so a throwing addon cannot break the remaining listeners or the domain dispatch.
 */
internal class MovementEventSinks {

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
