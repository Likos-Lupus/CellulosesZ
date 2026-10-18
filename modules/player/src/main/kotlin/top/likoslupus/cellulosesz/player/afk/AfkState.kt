package top.likoslupus.cellulosesz.player.afk

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** Why an AFK player should be reactivated, matching one `cancelOn*` setting. */
internal enum class AfkCancelTrigger {

    INTERACT,
    CHAT,
    FISH,

}

/**
 * Process-wide snapshot of online players' AFK state plus the interaction signals the movement and
 * fishing mixins raise. A tiny static bridge is required because a mixin has no reference to the
 * composition graph.
 *
 * [AfkService] is the only writer of the AFK set (on the server thread); the mixins read
 * [isAfk]/[shouldFreeze] and submit [AfkCancelTrigger] requests through [requestActivity], which the
 * feature drains on the next player tick so every decision and announcement stays on the server
 * thread.
 */
internal object AfkState {

    @Volatile private var afk: Set<UUID> = emptySet()

    @Volatile private var freezeWhileAfk: Boolean = false

    private val requests: Map<AfkCancelTrigger, MutableSet<UUID>> =
        AfkCancelTrigger.entries.associateWith { ConcurrentHashMap.newKeySet<UUID>() }

    /** Publishes the current server-thread AFK set; callers must be on the server thread. */
    fun publish(afkIds: Set<UUID>) {
        afk = HashSet(afkIds)
    }

    fun setFreezeWhileAfk(enabled: Boolean) {
        freezeWhileAfk = enabled
    }

    fun isAfk(playerId: UUID): Boolean =
        playerId in afk

    /** True when a frozen AFK player's movement packets must be cancelled. */
    fun shouldFreeze(playerId: UUID): Boolean =
        freezeWhileAfk && playerId in afk

    /** Records an interaction that may clear AFK; safe from the server-thread packet handlers. */
    fun requestActivity(playerId: UUID, trigger: AfkCancelTrigger) {
        requests.getValue(trigger).add(playerId)
    }

    /** Convenience for the interaction mixin, which never sees the trigger enum. */
    fun requestInteract(playerId: UUID) {
        requestActivity(playerId, AfkCancelTrigger.INTERACT)
    }

    /** Convenience for the fishing mixin, which never sees the trigger enum. */
    fun requestFish(playerId: UUID) {
        requestActivity(playerId, AfkCancelTrigger.FISH)
    }

    /** Removes and returns every pending trigger for one player. Server thread only. */
    fun drainActivity(playerId: UUID): Set<AfkCancelTrigger> {
        val drained = EnumSet.noneOf(AfkCancelTrigger::class.java)
        AfkCancelTrigger.entries.forEach { trigger ->
            if (requests.getValue(trigger).remove(playerId)) {
                drained.add(trigger)
            }
        }
        return drained
    }

    fun clear() {
        afk = emptySet()
        requests.values.forEach { it.clear() }
    }

}
