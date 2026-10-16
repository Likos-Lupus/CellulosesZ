package top.likoslupus.cellulosesz.api

import top.likoslupus.cellulosesz.api.extension.ExtensionRegistrationListener

/** Lifecycle of the API provider across the whole mod JVM. */
public enum class ApiAvailability {

    NOT_READY,
    READY,
    STOPPING,
    STOPPED,

}

/**
 * The loader-native entry point a consumer obtains (through the loader's own discovery mechanism)
 * to reach the API.
 *
 * [availability] is a cheap, side-effect-free snapshot. [whenReady] invokes the listener once, when
 * the API is ready for a server session; if it is already ready the listener is invoked immediately,
 * and if no session exists yet the registration is queued for the next one. The listener must not
 * touch world state directly — schedule onto the server thread first. [whenReady] is single-shot: a
 * consumer that wants to follow multiple integrated-world sessions registers again on each.
 *
 * [whenRegistrationOpen] opens the startup contribution window for policy/help extensions. It is
 * independent of [whenReady] (no server session is required) and should be used before the first
 * player action, so a protection rule is never registered after the fact.
 */
public interface CellulosesZApiProvider {

    public fun availability(): ApiAvailability

    public fun whenReady(listener: ApiReadyListener): Subscription

    public fun whenRegistrationOpen(listener: ExtensionRegistrationListener): Subscription

}
