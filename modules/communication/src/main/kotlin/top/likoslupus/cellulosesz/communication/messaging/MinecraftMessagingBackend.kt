package top.likoslupus.cellulosesz.communication.messaging

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
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
            else -> player.toOnlineIdentity()
        }

    override fun onlineIdentityById(id: UUID): OnlinePlayerIdentity? =
        when (
            val player = kernel.requireServer().playerList.getPlayer(id)
        ) {
            null -> null
            else -> player.toOnlineIdentity()
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

internal fun ServerPlayer.toOnlineIdentity(): OnlinePlayerIdentity =
    OnlinePlayerIdentity(uuid, gameProfile.name)
