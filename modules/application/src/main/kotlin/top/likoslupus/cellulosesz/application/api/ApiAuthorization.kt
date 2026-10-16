package top.likoslupus.cellulosesz.application.api

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.application.config.ApiIntegrationSettings

/**
 * Read-only capability authorization for addons, backed by the live config snapshot. System
 * operations are denied by default: a capability is only granted when the server explicitly lists it
 * for that addon id under `[api.integrations]`.
 */
internal class ApiAuthorization(
    private val settings: () -> ApiIntegrationSettings,
) {

    fun isSystemEnabled(): Boolean =
        settings().enabled

    fun capabilitiesFor(addonId: String): Set<String> {
        val current = settings()
        return when {
            !current.enabled -> emptySet()
            else -> current.addons[addonId]?.allowedCapabilities?.toSet()
                ?: emptySet()
        }
    }

    fun access(addonId: String): AddonAccess =
        AddonAccessImpl(addonId, this)

}

/** A live capability handle; [isAllowed] re-reads the current snapshot on every call. */
internal class AddonAccessImpl(
    override val addonId: String,
    private val authorization: ApiAuthorization,
) : AddonAccess {

    override fun isAllowed(capability: String): Boolean =
        capability in authorization.capabilitiesFor(addonId)

}
