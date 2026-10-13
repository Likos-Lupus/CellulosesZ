package top.likoslupus.cellulosesz.administration.jail

import java.util.*

/**
 * Process-wide snapshot of currently confined players, read by
 * [top.likoslupus.cellulosesz.administration.mixin.CommandsRestrictionMixin] on the command path and
 * written only by [JailService]. A tiny static bridge is required because the mixin has no reference
 * to the composition graph.
 */
internal object JailState {

    @Volatile private var confined: Set<UUID> = emptySet()

    fun update(ids: Set<UUID>) {
        confined = HashSet(ids)
    }

    fun clear() {
        confined = emptySet()
    }

    fun confined(): Set<UUID> = confined

}
