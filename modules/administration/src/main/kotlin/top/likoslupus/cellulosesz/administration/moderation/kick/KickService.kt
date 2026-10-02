package top.likoslupus.cellulosesz.administration.moderation.kick

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationTarget
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.protection.ProtectionResult
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

internal sealed interface KickResult {

    data class Kicked(
        val target: PlayerIdentity,
        val auditRecorded: Boolean,
    ) : KickResult

    data object TargetOffline : KickResult
    data object SelfTarget : KickResult
    data object TargetProtected : KickResult

}

internal data class KickAllResult(
    val kicked: Int,
    val protected: Int,
    val skippedIssuer: Boolean,
)

/** Kicks online players only; kick has no persistence, so this stays a thin orchestration. */
internal class KickService(
    private val kernel: RuntimeKernel,
    private val accountResolver: MinecraftAccountResolver,
    private val protection: TargetProtectionPolicy,
    private val audit: ModerationAuditService,
) {

    suspend fun kick(
        actor: ModerationActor,
        rawTarget: String,
        reason: ModerationReason,
    ): KickResult {
        val target = kernel.onServerThread { accountResolver.resolveOnline(rawTarget) }
            ?: return KickResult.TargetOffline

        when (kernel.onServerThread { protection.check(actor, target) }) {
            ProtectionResult.SELF -> return KickResult.SelfTarget
            ProtectionResult.PROTECTED -> return KickResult.TargetProtected
            ProtectionResult.ALLOWED -> Unit
        }

        kernel.onServerThread { disconnect(target, reason.value) }

        val recorded = audit.record(
            actor = actor,
            action = ModerationAuditAction.KICK,
            target = target,
            reason = reason.value
        )
        return KickResult.Kicked(
            target,
            recorded
        )
    }

    suspend fun kickAll(
        actor: ModerationActor,
        reason: ModerationReason,
    ): KickAllResult {
        val issuerId = (actor as? ModerationActor.Player)?.id

        val (targets, protectedCount, skippedIssuer) = kernel.onServerThread {
            var protectedCount = 0
            var skipped = false
            val targets = mutableListOf<PlayerIdentity>()

            kernel.requireServer().playerList.players.forEach { player ->
                if (issuerId != null && player.uuid == issuerId) {
                    skipped = true
                    return@forEach
                }
                val identity = PlayerIdentity(player.uuid, player.gameProfile.name)
                when (protection.check(actor, identity)) {
                    ProtectionResult.PROTECTED -> protectedCount++
                    ProtectionResult.ALLOWED,
                    ProtectionResult.SELF -> targets += identity
                }
            }
            Triple(targets, protectedCount, skipped)
        }

        kernel.onServerThread {
            targets.forEach { disconnect(it, reason.value) }
        }

        audit.record(
            actor = actor,
            action = ModerationAuditAction.KICK_ALL,
            target = PersistedModerationTarget(
                type = "server",
                id = null,
                name = "all",
            ),
            reason = reason.value,
            details = mapOf(
                "kickedCount" to targets.size.toString(),
                "protectedCount" to protectedCount.toString(),
                "skippedIssuer" to skippedIssuer.toString(),
            ),
        )

        return KickAllResult(
            targets.size,
            protectedCount,
            skippedIssuer
        )
    }

    private fun disconnect(identity: PlayerIdentity, reason: String) {
        kernel.requireServer()
                .playerList
                .getPlayer(identity.id)
                ?.connection
                ?.disconnect(Component.literal(reason))
    }

}
