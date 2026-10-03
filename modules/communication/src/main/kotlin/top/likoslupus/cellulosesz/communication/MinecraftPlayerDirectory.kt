package top.likoslupus.cellulosesz.communication

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.communication.messaging.OnlinePlayerIdentity
import top.likoslupus.cellulosesz.communication.messaging.toOnlineIdentity
import top.likoslupus.cellulosesz.core.command.canUseModeratorCommands
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/** Minecraft-backed [PlayerDirectory]. All operations require the server thread. */
internal class MinecraftPlayerDirectory(
    private val kernel: RuntimeKernel,
) : PlayerDirectory {

    override fun online(): List<OnlinePlayerIdentity> =
        kernel.requireServer().playerList.players
                .map { it.toOnlineIdentity() }

    override fun moderators(): List<OnlinePlayerIdentity> =
        kernel.requireServer().playerList.players
                .filter { it.createCommandSourceStack().canUseModeratorCommands() }
                .map { it.toOnlineIdentity() }

    override fun onlineIn(dimensionId: String): List<OnlinePlayerIdentity> =
        kernel.requireServer().playerList.players
                .filter { it.level().dimension().identifier().toString() == dimensionId }
                .map { it.toOnlineIdentity() }

    override fun send(playerId: UUID, message: Component): Boolean {
        val player = kernel.requireServer().playerList.getPlayer(playerId)
            ?: return false
        player.sendSystemMessage(message)
        return true
    }

}
