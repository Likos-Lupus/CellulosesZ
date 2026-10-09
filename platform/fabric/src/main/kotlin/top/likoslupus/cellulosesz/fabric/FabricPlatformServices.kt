package top.likoslupus.cellulosesz.fabric

import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.application.bootstrap.PlatformServices
import top.likoslupus.cellulosesz.core.permission.PermissionBridge
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.vanillaFallback

/**
 * Fabric permission integration through the fabric-permissions-api, which transparently supports
 * LuckPerms and other providers, and falls back to vanilla when no provider is installed.
 */
class FabricPermissionBridge : PermissionBridge {

    override val backendName: String get() = "fabric-permissions-api"

    override fun test(
        source: CommandSourceStack,
        spec: PermissionSpec
    ): Boolean =
        Permissions.check(
            source,
            spec.node,
            vanillaFallback(source, spec)
        )

}

class FabricPlatformServices : PlatformServices {

    override val loaderName: String
        get() = "fabric"

    override val permissionBridge: PermissionBridge = FabricPermissionBridge()

}
