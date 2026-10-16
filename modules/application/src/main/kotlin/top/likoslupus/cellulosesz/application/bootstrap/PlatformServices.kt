package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.api.CellulosesZApiProvider
import top.likoslupus.cellulosesz.core.permission.PermissionBridge
import top.likoslupus.cellulosesz.core.permission.PermissionInspector

/**
 * Loader-supplied services. Constructed in the thin platform shim so the composition root never
 * touches a loader or third-party permission API directly.
 */
public interface PlatformServices {

    public val loaderName: String

    public val permissionBridge: PermissionBridge

    /**
     * Native permission diagnostics, or null when the loader has no native provider installed. The
     * composition root uses this only for the `/cellulosesz permissions` diagnostic command; the
     * effective authorization always goes through [permissionBridge].
     */
    public val permissionInspector: PermissionInspector?
        get() = null

    /**
     * Publishes the public-API provider through this loader's native discovery mechanism. Called once,
     * after the provider is attached and before the server session becomes ready. A loader that has no
     * verified discovery path may log a warning and expose nothing (the contract is still complete).
     */
    public fun publishApi(provider: CellulosesZApiProvider)

}
