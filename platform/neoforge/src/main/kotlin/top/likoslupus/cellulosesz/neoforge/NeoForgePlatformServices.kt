package top.likoslupus.cellulosesz.neoforge

import net.minecraft.commands.CommandSourceStack
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.server.permission.PermissionAPI
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent
import net.neoforged.neoforge.server.permission.nodes.PermissionNode
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes
import top.likoslupus.cellulosesz.api.CellulosesZApiProvider
import top.likoslupus.cellulosesz.application.bootstrap.PlatformServices
import top.likoslupus.cellulosesz.core.permission.*
import top.likoslupus.cellulosesz.neoforge.api.CellulosesZApiExtension
import top.likoslupus.cellulosesz.platform.luckperms.LuckPermsPermissionInspector

/**
 * NeoForge permission integration. Every CellulosesZ node is registered with the NeoForge
 * PermissionAPI; a LuckPerms NeoForge provider (or any other handler) supplies the value, and the
 * node default reproduces vanilla behaviour when no provider expresses an opinion.
 */
internal object NeoForgePermissionNodes {

    fun register(): Map<String, PermissionNode<Boolean>> {
        val duplicates = CommandPermissions.all
                .groupingBy(PermissionSpec::node)
                .eachCount()
                .filterValues { it > 1 }
                .keys
        check(duplicates.isEmpty()) {
            "duplicate CellulosesZ permission nodes declared: ${duplicates.joinToString()}"
        }

        val nodes = CommandPermissions.all.associate { spec ->
            // Keep the node dot-separated. PermissionNode composes `modId + "." + nodeName`, so the
            // previous `.replace('.', '/')` produced `cellulosesz.command/tp` — a name no other
            // backend or Loader (Fabric, LuckPerms) ever sees or can grant.
            val local = PermissionNodeNames.localName(spec.node)
            val node = PermissionNode(
                PermissionNodeNames.MOD_ID,
                local,
                PermissionTypes.BOOLEAN,
                { player, _, _ ->
                    player != null && vanillaFallback(
                        player.createCommandSourceStack(),
                        spec
                    )
                },
            )
            check(node.nodeName == spec.node) {
                "NeoForge permission node mismatch: declared='${spec.node}' actual='${node.nodeName}'"
            }
            spec.node to node
        }
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes::class.java) {
            it.addNodes(nodes.values)
        }
        return nodes
    }
}

internal class NeoForgePermissionBridge(
    private val nodes: Map<String, PermissionNode<Boolean>>,
) : PermissionBridge {

    override val backendName: String
        get() = "neoforge-permission-api"

    override fun test(
        source: CommandSourceStack,
        spec: PermissionSpec
    ): Boolean {
        val node = nodes[spec.node]
        val player = source.player
        return when {
            node == null || player == null -> vanillaFallback(source, spec)
            else -> PermissionAPI.getPermission(player, node)
        }
    }
}

class NeoForgePlatformServices(
    private val container: ModContainer,
) : PlatformServices {

    private val nodes: Map<String, PermissionNode<Boolean>> = NeoForgePermissionNodes.register()

    override val loaderName: String
        get() = "neoforge"

    override val permissionBridge: PermissionBridge = NeoForgePermissionBridge(nodes)

    override val permissionInspector: PermissionInspector? by lazy {
        when {
            ModList.get().isLoaded("luckperms") -> LuckPermsPermissionInspector(permissionBridge)
            else -> null
        }
    }

    /**
     * Publishes the API provider through the FML extension-point mechanism on the `cellulosesz`
     * `ModContainer`. This is the loader-native, reflection-free discovery path: a consumer looks up
     * the container by mod id and calls `getCustomExtension(CellulosesZApiExtension.class)`. The same
     * pure-JVM [CellulosesZApiProvider] is returned on every loader.
     */
    override fun publishApi(provider: CellulosesZApiProvider) {
        container.registerExtensionPoint(
            CellulosesZApiExtension::class.java,
            CellulosesZApiExtension { provider },
        )
    }

}
