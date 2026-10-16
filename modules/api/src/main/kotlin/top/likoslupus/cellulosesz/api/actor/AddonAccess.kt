package top.likoslupus.cellulosesz.api.actor

/**
 * A capability handle returned by the registration window. It reports the abilities the server
 * configuration has granted to this addon.
 *
 * [isAllowed] is a live check against the current authorization snapshot, not a cached permanent
 * grant. A privileged system operation may still be rejected at commit time if the configuration has
 * since changed.
 */
public interface AddonAccess {

    public val addonId: String

    public fun isAllowed(capability: String): Boolean

}
