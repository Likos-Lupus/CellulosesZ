package top.likoslupus.cellulosesz.administration.playerstate

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.GameType
import top.likoslupus.cellulosesz.administration.operator.PlayerGameMode
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/** Minecraft implementation of the player-state toggles. Server-thread confined. */
internal class MinecraftPlayerStateBackend(
    private val kernel: RuntimeKernel,
) : PlayerStateBackend {

    override fun applyHeal(playerId: UUID, removeEffects: Boolean): Boolean {
        val player = player(playerId)
            ?: return false
        player.health = player.maxHealth
        if (removeEffects) {
            player.removeAllEffects()
        }
        return true
    }

    override fun isInvulnerable(playerId: UUID): Boolean =
        player(playerId)?.isInvulnerable
            ?: false

    override fun setInvulnerable(playerId: UUID, value: Boolean): Boolean {
        val player = player(playerId)
            ?: return false
        player.isInvulnerable = value
        return true
    }

    override fun mayFly(playerId: UUID): Boolean =
        player(playerId)?.abilities?.mayfly
            ?: false

    override fun setFly(playerId: UUID, value: Boolean): Boolean {
        val player = player(playerId)
            ?: return false
        val abilities = player.abilities
        abilities.mayfly = value
        abilities.flying = value
        player.onUpdateAbilities()
        return true
    }

    override fun dimension(playerId: UUID): String? =
        player(playerId)?.level()?.dimension()?.identifier()?.toString()

    override fun gameMode(playerId: UUID): PlayerGameMode? =
        when (player(playerId)?.gameMode()) {
            GameType.SURVIVAL -> PlayerGameMode.SURVIVAL
            GameType.CREATIVE -> PlayerGameMode.CREATIVE
            GameType.ADVENTURE -> PlayerGameMode.ADVENTURE
            GameType.SPECTATOR -> PlayerGameMode.SPECTATOR
            null -> null
        }

    private fun player(playerId: UUID): ServerPlayer? =
        kernel.requireServer().playerList.getPlayer(playerId)

}
