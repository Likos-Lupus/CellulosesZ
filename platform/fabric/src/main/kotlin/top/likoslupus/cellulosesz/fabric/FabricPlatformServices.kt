package top.likoslupus.cellulosesz.fabric

import net.fabricmc.fabric.api.permission.v1.PermissionNode
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.api.CellulosesZApiProvider
import top.likoslupus.cellulosesz.application.bootstrap.PlatformServices
import top.likoslupus.cellulosesz.core.permission.PermissionBridge
import top.likoslupus.cellulosesz.core.permission.PermissionInspector
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.vanillaFallback
import top.likoslupus.cellulosesz.platform.luckperms.LuckPermsPermissionInspector

/**
 * Fabric permission integration through the loader's own `fabric-permission-api` (part of Fabric
 * API). Providers such as LuckPerms answer through `PermissionEvents`; when nobody answers, the
 * check falls back to the vanilla permission level.
 *
 * The API keys nodes as `namespace:path`, and providers render them as `namespace.path`, so a
 * `cellulosesz.command.home` spec is split into (`cellulosesz`, `command.home`) and reads the same
 * as before in a permissions plugin.
 */
class FabricPermissionBridge : PermissionBridge {

    override val backendName: String get() = "fabric-permission-api"

    override fun test(
        source: CommandSourceStack,
        spec: PermissionSpec
    ): Boolean {
        val fallback = vanillaFallback(source, spec)
        return source.checkPermission(
            node(spec.node),
            fallback
        ) ?: fallback
    }

    private fun node(raw: String): PermissionNode<Boolean> {
        val separator = raw.indexOf('.')
        val namespace = if (separator > 0) raw.substring(0, separator) else MOD_ID
        val path = if (separator > 0) raw.substring(separator + 1) else raw
        return PermissionNode.of(namespace, path.replace(':', '.'))
    }

    private companion object {

        private const val MOD_ID = "cellulosesz"

    }

}

class FabricPlatformServices : PlatformServices {

    override val loaderName: String
        get() = "fabric"

    override val permissionBridge: PermissionBridge = FabricPermissionBridge()

    /**
     * A native LuckPerms inspector when LuckPerms is loaded, else null. The inspector class is only
     * instantiated here (not merely referenced), so a server without LuckPerms never links it.
     */
    override val permissionInspector: PermissionInspector? by lazy {
        if (FabricLoader.getInstance().isModLoaded("luckperms")) {
            LuckPermsPermissionInspector(permissionBridge)
        } else {
            null
        }
    }

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
