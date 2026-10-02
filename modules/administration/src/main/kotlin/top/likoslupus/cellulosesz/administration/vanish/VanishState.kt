package top.likoslupus.cellulosesz.administration.vanish

import java.util.*

/**
 * Process-wide snapshot of vanished players, read by the tracking mixin on the server thread and
 * written only by [VanishService]. A tiny static bridge is required because the mixin has no
 * reference to the composition graph.
 */
internal object VanishState {

    @Volatile private var vanished: Set<UUID> = emptySet()

    fun update(ids: Set<UUID>) {
        vanished = HashSet(ids)
    }

    fun clear() {
        vanished = emptySet()
    }

    fun isVanished(playerId: UUID): Boolean = vanished.contains(playerId)

    /** Vanished players stay visible to other vanished players. */
    fun shouldHide(subjectId: UUID, viewerId: UUID): Boolean =
        vanished.contains(subjectId) && !vanished.contains(viewerId)

}
