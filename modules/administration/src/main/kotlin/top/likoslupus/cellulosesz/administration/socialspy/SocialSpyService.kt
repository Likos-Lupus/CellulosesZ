package top.likoslupus.cellulosesz.administration.socialspy

import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.util.*

internal data class SocialSpyResult(
    val enabled: Boolean,
    val auditRecorded: Boolean,
)

/**
 * Session-only social spy state. It is never persisted and is cleared when a player disconnects, so
 * a moderator can never silently remain a spy across restarts by accident.
 */
internal class SocialSpyService(
    private val kernel: RuntimeKernel,
    private val audit: ModerationAuditService,
) {

    private val spies = HashSet<UUID>()

    /** Server-thread confined. */
    fun isSpying(playerId: UUID): Boolean = spies.contains(playerId)

    /** Server-thread confined. Returns online spy recipients excluding the conversation parties. */
    fun recipients(exclude: Set<UUID>): List<UUID> =
        spies.filter { it !in exclude }

    fun clear(playerId: UUID) {
        spies.remove(playerId)
    }

    suspend fun set(
        actor: ModerationActor,
        identity: PlayerIdentity,
        enable: Boolean,
    ): SocialSpyResult {
        kernel.onServerThread {
            when {
                enable -> spies.add(identity.id)
                else -> spies.remove(identity.id)
            }
        }
        val action = when {
            enable -> ModerationAuditAction.SOCIAL_SPY_ENABLE
            else -> ModerationAuditAction.SOCIAL_SPY_DISABLE
        }
        val recorded = audit.record(actor, action, identity)
        return SocialSpyResult(enable, recorded)
    }

}
