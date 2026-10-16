package top.likoslupus.cellulosesz.api.common

/**
 * A handle to one in-flight business request.
 *
 * [cancel] attempts to cancel the request. It returns `true` only if the cancellation won **before**
 * the irreversible commit point (for example, before the Minecraft teleport call or the point of no
 * return in a reward delivery). Once a move or delivery has committed, [cancel] returns `false` and
 * must not retroactively mark the operation cancelled.
 *
 * This is deliberately distinct from [top.likoslupus.cellulosesz.api.Subscription.close], which
 * only unregisters an event/registration listener. Calling [cancel] is optional; a request completes
 * normally without it.
 */
public fun interface ApiRequestHandle {

    public fun cancel(): Boolean

}
