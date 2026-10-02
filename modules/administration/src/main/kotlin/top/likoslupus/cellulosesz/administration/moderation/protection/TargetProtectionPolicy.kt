package top.likoslupus.cellulosesz.administration.moderation.protection

import net.minecraft.server.players.NameAndId
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/** The outcome of the protection policy for a single target. */
internal enum class ProtectionResult {

    ALLOWED,
    SELF,
    PROTECTED,

}

/**
 * The minimal safety floor for the current vanilla permission model. Console always bypasses; a
 * player moderator cannot act on an operator when `protectOperators` is set. This is not a
 * permission hierarchy and must not grow into one.
 *
 * Must be invoked on the server thread (reads [net.minecraft.server.players.PlayerList]).
 */
internal class TargetProtectionPolicy(
    private val kernel: RuntimeKernel,
    private val settings: () -> ModerationSettings,
) {

    fun check(
        actor: ModerationActor,
        target: PlayerIdentity,
    ): ProtectionResult =
        when {
            actor is ModerationActor.Player && actor.id == target.id ->
                ProtectionResult.SELF

            actor !is ModerationActor.Player ->
                ProtectionResult.ALLOWED

            !settings().protectOperators ->
                ProtectionResult.ALLOWED

            kernel.requireServer().playerList.isOp(NameAndId(target.id, target.name)) ->
                ProtectionResult.PROTECTED

            else -> ProtectionResult.ALLOWED
        }

}
