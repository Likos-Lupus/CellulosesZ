package top.likoslupus.cellulosesz.utility.kit

import java.time.Duration

/**
 * How often a kit may be claimed. An explicit sealed policy replaces the legacy
 * `delay < 0 = once / 0 = unlimited / > 0 = cooldown` encoding.
 */
internal sealed interface KitReusePolicy {

    data object Always : KitReusePolicy

    data object Once : KitReusePolicy

    data class Cooldown(val duration: Duration) : KitReusePolicy

}
