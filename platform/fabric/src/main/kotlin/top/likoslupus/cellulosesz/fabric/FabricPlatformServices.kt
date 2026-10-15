package top.likoslupus.cellulosesz.fabric

import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.api.CellulosesZApiProvider
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

    /**
     * Publishes the provider on the Fabric loader's ObjectShare under a namespaced key. Consumers read
     * it with `ObjectShare.whenAvailable`, which is the loader-native, reflection-free discovery path;
     * the callback may run on an arbitrary thread, so consumers must schedule world access onto the
     * server thread themselves.
     */
    override fun publishApi(provider: CellulosesZApiProvider) {
        FabricLoader.getInstance().objectShare.put(API_KEY, provider)
    }

    private companion object {

        private const val API_KEY = "cellulosesz:api-v0"

    }

}
