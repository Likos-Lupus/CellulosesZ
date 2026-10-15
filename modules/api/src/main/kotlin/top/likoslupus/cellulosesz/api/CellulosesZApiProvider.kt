package top.likoslupus.cellulosesz.api

/** Lifecycle of the API provider across the whole mod JVM. */
public enum class ApiAvailability {

    NOT_READY,
    READY,
    STOPPING,
    STOPPED,

}

/**
 * The loader-native entry point a consumer obtains (through the loader's own discovery mechanism)
 * to reach the API. [availability] is a cheap, side-effect-free snapshot. [whenReady] invokes the
 * listener once, when the API is ready for a server session; if it is already ready the listener is
 * invoked immediately. The listener must not touch world state directly — schedule onto the server
 * thread first.
 */
public interface CellulosesZApiProvider {

    public fun availability(): ApiAvailability

    public fun whenReady(listener: ApiReadyListener): Subscription

}
