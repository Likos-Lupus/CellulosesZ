package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.api.CellulosesZApiProvider
import top.likoslupus.cellulosesz.core.permission.PermissionBridge

/**
 * Loader-supplied services. Constructed in the thin platform shim so the composition root never
 * touches a loader or third-party permission API directly.
 */
public interface PlatformServices {

    public val loaderName: String

    public val permissionBridge: PermissionBridge

    /**
     * Publishes the public-API provider through this loader's native discovery mechanism. Called once,
     * after the provider is attached and before the server session becomes ready. A loader that has no
     * verified discovery path may log a warning and expose nothing (the contract is still complete).
     */
    public fun publishApi(provider: CellulosesZApiProvider)

}
