package top.likoslupus.cellulosesz.administration.jail

/**
 * Process-wide, single-purpose bridge holding the current [JailRestrictionSnapshot]. It is read by
 * [top.likoslupus.cellulosesz.administration.mixin.CommandsRestrictionMixin] on the command path and
 * written only by [top.likoslupus.cellulosesz.administration.AdministrationFeature]. A tiny static
 * bridge is required because the mixin has no reference to the composition graph; it is a value
 * holder, never a service locator.
 */
internal object RestrictCommandsState {

    @Volatile
    private var snapshot: JailRestrictionSnapshot = JailRestrictionSnapshot.NONE

    fun set(value: JailRestrictionSnapshot) {
        snapshot = value
    }

    fun current(): JailRestrictionSnapshot = snapshot

    fun clear() {
        snapshot = JailRestrictionSnapshot.NONE
    }

}
