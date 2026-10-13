package top.likoslupus.cellulosesz.administration.playerstate

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.Attributes
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

/** Minecraft implementation of operator experience/speed adjustments. Server-thread confined. */
internal class MinecraftPlayerAdjustmentBackend(
    private val kernel: RuntimeKernel,
) : PlayerAdjustmentBackend {

    override fun experience(playerId: UUID): ExperienceSnapshot? =
        player(playerId)?.let(::snapshot)

    override fun applyExperienceDelta(playerId: UUID, delta: Int): ExperienceSnapshot? {
        val player = player(playerId)
            ?: return null
        player.giveExperiencePoints(delta)
        return snapshot(player)
    }

    override fun readSpeed(playerId: UUID): SpeedSnapshot? {
        val player = player(playerId)
            ?: return null
        val walk = player.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue
            ?: return null
        val fly = player.getAttribute(Attributes.FLYING_SPEED)?.baseValue
            ?: return null
        return SpeedSnapshot(walk, fly)
    }

    override fun writeSpeed(playerId: UUID, value: SpeedSnapshot) {
        val player = player(playerId)
            ?: return
        player.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue = value.walk
        player.getAttribute(Attributes.FLYING_SPEED)?.baseValue = value.fly
    }

    private fun player(playerId: UUID): ServerPlayer? =
        kernel.requireServer().playerList.getPlayer(playerId)

    private fun snapshot(player: ServerPlayer): ExperienceSnapshot =
        ExperienceSnapshot(
            level = player.experienceLevel,
            total = player.totalExperience,
            needed = player.xpNeededForNextLevel,
        )

}
