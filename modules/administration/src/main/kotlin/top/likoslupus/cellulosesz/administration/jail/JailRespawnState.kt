package top.likoslupus.cellulosesz.administration.jail

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide set of players who have just respawned and need an immediate confinement re-check.
 * Written by [top.likoslupus.cellulosesz.administration.mixin.PlayerListRespawnMixin] on the server
 * thread and drained by [top.likoslupus.cellulosesz.administration.AdministrationFeature]. A tiny
 * static bridge is required because the mixin has no reference to the composition graph.
 */
internal object JailRespawnState {

    private val pending: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    fun mark(playerId: UUID) {
        pending += playerId
    }

    fun consume(playerId: UUID): Boolean =
        pending.remove(playerId)

    fun clear() {
        pending.clear()
    }

}
