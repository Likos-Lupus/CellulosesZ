package top.likoslupus.cellulosesz.api.common

/**
 * Receives one immutable domain event. Events describe facts that already happened; a listener can
 * never veto or reverse them. A throwing listener is isolated, logged, and never stops the others.
 *
 * Events are dispatched on the Minecraft server thread while the session is active.
 */
public fun interface ApiEventListener<in T> {

    public fun onEvent(event: T)

}
