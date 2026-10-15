package top.likoslupus.cellulosesz.neoforge

import net.minecraft.commands.CommandSourceStack
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.server.permission.PermissionAPI
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent
import net.neoforged.neoforge.server.permission.nodes.PermissionNode
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.CellulosesZApiProvider
import top.likoslupus.cellulosesz.application.bootstrap.PlatformServices
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionBridge
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.vanillaFallback

/**
 * NeoForge permission integration. Every CellulosesZ node is registered with the NeoForge
 * PermissionAPI; a LuckPerms NeoForge provider (or any other handler) supplies the value, and the
 * node default reproduces vanilla behaviour when no provider expresses an opinion.
 */
internal object NeoForgePermissionNodes {

    fun register(): Map<String, PermissionNode<Boolean>> {
        val nodes = CommandPermissions.all.associate { spec ->
            val path = spec.node
                    .removePrefix("cellulosesz.")
                    .replace('.', '/')
            val node = PermissionNode(
                "cellulosesz",
                path,
                PermissionTypes.BOOLEAN,
                { player, _, _ ->
                    player != null && vanillaFallback(
                        player.createCommandSourceStack(),
                        spec
                    )
                },
            )
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

class NeoForgePlatformServices : PlatformServices {

    private val nodes: Map<String, PermissionNode<Boolean>> = NeoForgePermissionNodes.register()

    override val loaderName: String
        get() = "neoforge"

    override val permissionBridge: PermissionBridge = NeoForgePermissionBridge(nodes)

    /**
     * NeoForge API discovery is not yet verified for this target. The contract is complete and Fabric
     * publishes through ObjectShare, but the NeoForge extension/discovery path must first be spiked
     * against the exact 26.1.x NeoForge API (see docs/adr/0016). Until then the provider is not
     * exposed, so NeoForge consumers must not treat the API as available.
     */
    override fun publishApi(provider: CellulosesZApiProvider) {
        LOGGER.warn(
            "CellulosesZ public API v0 is not exposed on NeoForge yet: the loader-native discovery " +
                    "path is pending a target-version spike (see docs/adr/0016). Fabric publishes normally."
        )
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(NeoForgePlatformServices::class.java)

    }

}
