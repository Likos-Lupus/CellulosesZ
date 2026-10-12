package top.likoslupus.cellulosesz.communication.messaging

import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import java.util.*

/**
 * The only communication file that resolves Minecraft players and pushes private messages to them.
 * All operations require the server thread.
 */
internal class MinecraftMessagingBackend(
    private val kernel: RuntimeKernel,
    private val adventure: AdventureRuntime,
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

    override fun deliverPrivateMessage(playerId: UUID, messages: List<Component>): Boolean =
        when (
            kernel.requireServer().playerList.getPlayer(playerId)
        ) {
            null -> false
            else -> {
                val audience = adventure.audience(CommandFeedbackTarget.Player(playerId))
                messages.forEach(audience::sendMessage)
                true
            }
        }

}

internal fun ServerPlayer.toOnlineIdentity(): OnlinePlayerIdentity =
    OnlinePlayerIdentity(uuid, gameProfile.name)
