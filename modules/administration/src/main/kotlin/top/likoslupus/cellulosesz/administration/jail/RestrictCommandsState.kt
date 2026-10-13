package top.likoslupus.cellulosesz.administration.jail

/**
 * Process-wide snapshot of whether confined players are limited to the allowed commands, read by
 * [top.likoslupus.cellulosesz.administration.mixin.CommandsRestrictionMixin] and written by
 * [top.likoslupus.cellulosesz.administration.AdministrationFeature] from `[administration.jail]`.
 * A tiny static bridge is required because the mixin has no reference to the composition graph.
 */
internal object RestrictCommandsState {

    @Volatile private var enabled: Boolean = false

    fun set(value: Boolean) {
        enabled = value
    }

    fun isEnabled(): Boolean = enabled

}
