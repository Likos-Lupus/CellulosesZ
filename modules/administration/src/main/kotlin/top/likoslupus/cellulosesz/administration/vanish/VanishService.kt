package top.likoslupus.cellulosesz.administration.vanish

import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

internal data class VanishResult(
    val enabled: Boolean,
    val auditRecorded: Boolean,
)

/**
 * Session-only vanish. Entity tracking is suppressed by the mixin consulting [VanishState]; this
 * service owns the vanished set and the tab-list packets. It never uses invisibility effects.
 */
internal class VanishService(
    private val kernel: RuntimeKernel,
    private val audit: ModerationAuditService,
) {

    private val vanished = LinkedHashSet<UUID>()

    fun isVanished(playerId: UUID): Boolean = vanished.contains(playerId)

    fun clear(playerId: UUID) {
        if (vanished.remove(playerId)) {
            VanishState.update(vanished)
        }
    }

    fun clearAll() {
        vanished.clear()
        VanishState.clear()
    }

    suspend fun set(
        actor: ModerationActor,
        identity: PlayerIdentity,
        enable: Boolean,
    ): VanishResult {
        kernel.onServerThread {
            when {
                enable -> vanished.add(identity.id)
                else -> vanished.remove(identity.id)
            }
            VanishState.update(vanished)
            applyTabVisibility(identity.id, enable)
        }
        val action = when {
            enable -> ModerationAuditAction.VANISH_ENABLE
            else -> ModerationAuditAction.VANISH_DISABLE
        }
        val recorded = audit.record(actor, action, identity)
        return VanishResult(enable, recorded)
    }

    private fun applyTabVisibility(targetId: UUID, enable: Boolean) {
        val server = kernel.requireServer()
        val target = server.playerList.getPlayer(targetId)
            ?: return
        server.playerList.players
                .filter { it.uuid != targetId }
                .forEach { viewer ->
                    viewer.connection.send(
                        when {
                            enable -> ClientboundPlayerInfoRemovePacket(
                                listOf(targetId)
                            )

                            else -> ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(
                                listOf(target)
                            )
                        }
                    )
                }
    }

}
