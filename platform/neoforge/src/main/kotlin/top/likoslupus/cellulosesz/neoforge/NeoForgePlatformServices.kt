package top.likoslupus.cellulosesz.neoforge

import net.minecraft.commands.CommandSourceStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.server.permission.PermissionAPI
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent
import net.neoforged.neoforge.server.permission.nodes.PermissionNode
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes
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

    fun register(bus: IEventBus): Map<String, PermissionNode<Boolean>> {
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
        bus.addListener(PermissionGatherEvent.Nodes::class.java) {
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
    modEventBus: IEventBus,
) : PlatformServices {

    private val nodes: Map<String, PermissionNode<Boolean>> = NeoForgePermissionNodes.register(
        modEventBus
    )

    override val loaderName: String
        get() = "neoforge"

    override val permissionBridge: PermissionBridge = NeoForgePermissionBridge(nodes)

}
