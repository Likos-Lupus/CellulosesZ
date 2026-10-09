package top.likoslupus.cellulosesz.application.bootstrap

import top.likoslupus.cellulosesz.core.permission.PermissionBridge

/**
 * Loader-supplied services. Constructed in the thin platform shim so the composition root never
 * touches a loader or third-party permission API directly.
 */
public interface PlatformServices {

    public val loaderName: String

    public val permissionBridge: PermissionBridge

}
