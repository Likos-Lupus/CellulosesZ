package top.likoslupus.cellulosesz.administration.operator

import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.protection.ProtectionResult
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.core.runtime.KernelState
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

internal sealed interface PlayerControlResult {

    data class Killed(
        val target: PlayerIdentity,
        val auditRecorded: Boolean,
    ) : PlayerControlResult

    data class GameModeChanged(
        val target: PlayerIdentity,
        val mode: PlayerGameMode,
        val auditRecorded: Boolean,
    ) : PlayerControlResult

    data class SudoExecuted(
        val target: PlayerIdentity,
        val auditRecorded: Boolean,
    ) : PlayerControlResult

    data object TargetOffline : PlayerControlResult
    data object SelfTarget : PlayerControlResult
    data object TargetProtected : PlayerControlResult
    data object RuntimeStopping : PlayerControlResult

}

/** Operator control over online players: kill, game mode, and console-only sudo. */
internal class PlayerControlService(
    private val kernel: RuntimeKernel,
    private val backend: PlayerControlBackend,
    private val accountResolver: MinecraftAccountResolver,
    private val protection: TargetProtectionPolicy,
    private val audit: ModerationAuditService,
) {

    suspend fun kill(actor: ModerationActor, rawTarget: String): PlayerControlResult {
        if (isStopping()) return PlayerControlResult.RuntimeStopping
        val target = resolveOnline(rawTarget) ?: return PlayerControlResult.TargetOffline
        when (kernel.onServerThread { protection.check(actor, target) }) {
            ProtectionResult.SELF -> return PlayerControlResult.SelfTarget
            ProtectionResult.PROTECTED -> return PlayerControlResult.TargetProtected
            ProtectionResult.ALLOWED -> Unit
        }
        if (!kernel.onServerThread { backend.kill(target) }) return PlayerControlResult.TargetOffline
        val recorded = audit.record(actor, ModerationAuditAction.KILL, target)
        return PlayerControlResult.Killed(target, recorded)
    }

    suspend fun setGameMode(
        actor: ModerationActor,
        rawTarget: String?,
        mode: PlayerGameMode,
    ): PlayerControlResult {
        if (isStopping()) return PlayerControlResult.RuntimeStopping
        val name = rawTarget ?: (actor as? ModerationActor.Player)?.name
        ?: return PlayerControlResult.TargetOffline
        val target = resolveOnline(name) ?: return PlayerControlResult.TargetOffline
        when (kernel.onServerThread { protection.check(actor, target) }) {
            ProtectionResult.SELF -> Unit
            ProtectionResult.PROTECTED -> return PlayerControlResult.TargetProtected
            ProtectionResult.ALLOWED -> Unit
        }
        if (!kernel.onServerThread { backend.setGameMode(target, mode) }) {
            return PlayerControlResult.TargetOffline
        }
        val recorded = audit.record(actor, ModerationAuditAction.GAMEMODE, target)
        return PlayerControlResult.GameModeChanged(target, mode, recorded)
    }

    suspend fun executeAsPlayer(
        actor: ModerationActor,
        rawTarget: String,
        command: String,
    ): PlayerControlResult {
        if (isStopping()) return PlayerControlResult.RuntimeStopping
        val target = resolveOnline(rawTarget) ?: return PlayerControlResult.TargetOffline
        when (kernel.onServerThread { protection.check(actor, target) }) {
            ProtectionResult.SELF -> return PlayerControlResult.SelfTarget
            ProtectionResult.PROTECTED -> return PlayerControlResult.TargetProtected
            ProtectionResult.ALLOWED -> Unit
        }
        if (!kernel.onServerThread { backend.executeAsPlayer(target, command) }) {
            return PlayerControlResult.TargetOffline
        }
        val recorded = audit.record(actor, ModerationAuditAction.SUDO, target)
        return PlayerControlResult.SudoExecuted(target, recorded)
    }

    private suspend fun resolveOnline(rawTarget: String): PlayerIdentity? =
        kernel.onServerThread { accountResolver.resolveOnline(rawTarget) }

    private fun isStopping(): Boolean {
        val state = kernel.state
        return state == KernelState.STOPPING || state == KernelState.STOPPED
    }

}
