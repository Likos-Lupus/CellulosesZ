package top.likoslupus.cellulosesz.communication.action

/**
 * Process-wide snapshot of whether CellulosesZ replaces vanilla `/me`, read by
 * `EmoteCommandsMixin` while vanilla builds its dispatcher and written by the composition root
 * before the server starts. A tiny static bridge is required because the mixin has no reference to
 * the composition graph. It lives outside the `communication.mixin` package because Mixin forbids
 * normal code from referencing classes in a declared mixin package.
 */
internal object MeOverrideState {

    @Volatile private var enabled: Boolean = false

    fun set(value: Boolean) {
        enabled = value
    }

    fun isEnabled(): Boolean =
        enabled

}
