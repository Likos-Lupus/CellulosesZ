package top.likoslupus.cellulosesz.administration.operator

import net.minecraft.world.level.GameType
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** Minecraft implementation of operator player control. Must run on the server thread. */
internal class MinecraftPlayerControlBackend(
    private val kernel: RuntimeKernel,
) : PlayerControlBackend {

    override fun kill(target: PlayerIdentity): Boolean {
        val player = kernel.requireServer().playerList.getPlayer(target.id)
            ?: return false
        player.kill(player.level())
        return true
    }

    override fun setGameMode(target: PlayerIdentity, mode: PlayerGameMode): Boolean {
        val player = kernel.requireServer().playerList.getPlayer(target.id)
            ?: return false
        return player.setGameMode(mode.toGameType())
    }

    override fun executeAsPlayer(target: PlayerIdentity, command: String): Boolean {
        val server = kernel.requireServer()
        val player = server.playerList.getPlayer(target.id)
            ?: return false
        server.commands.performPrefixedCommand(player.createCommandSourceStack(), command)
        return true
    }

    private fun PlayerGameMode.toGameType(): GameType =
        when (this) {
            PlayerGameMode.SURVIVAL -> GameType.SURVIVAL
            PlayerGameMode.CREATIVE -> GameType.CREATIVE
            PlayerGameMode.ADVENTURE -> GameType.ADVENTURE
            PlayerGameMode.SPECTATOR -> GameType.SPECTATOR
        }

}
