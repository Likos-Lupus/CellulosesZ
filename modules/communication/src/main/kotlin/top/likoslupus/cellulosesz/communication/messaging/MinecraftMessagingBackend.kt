package top.likoslupus.cellulosesz.communication.messaging

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/**
 * The only communication file that resolves Minecraft players and pushes private messages to them.
 * All operations require the server thread.
 */
internal class MinecraftMessagingBackend(
    private val kernel: RuntimeKernel,
) : MessagingBackend {

    override fun onlineIdentityByName(name: String): OnlinePlayerIdentity? =
        when (
            val player = kernel.requireServer().playerList.getPlayerByName(name)
        ) {
            null -> null
            else -> OnlinePlayerIdentity(
                player.uuid,
                player.gameProfile.name
            )
        }

    override fun onlineIdentityById(id: UUID): OnlinePlayerIdentity? =
        when (
            val player = kernel.requireServer().playerList.getPlayer(id)
        ) {
            null -> null
            else -> OnlinePlayerIdentity(
                player.uuid,
                player.gameProfile.name
            )
        }

    override fun deliverPrivateMessage(playerId: UUID, message: Component): Boolean =
        when (
            val player = kernel.requireServer().playerList.getPlayer(playerId)
        ) {
            null -> false
            else -> {
                player.sendSystemMessage(message)
                true
            }
        }

}
