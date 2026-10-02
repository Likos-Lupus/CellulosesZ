package top.likoslupus.cellulosesz.administration.moderation.ban

import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationTarget
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.TemporaryDuration
import top.likoslupus.cellulosesz.administration.moderation.protection.ProtectionResult
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.core.runtime.KernelState
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.time.Clock
import java.time.Instant

internal sealed interface BanResult {

    data class Applied(
        val target: PlayerIdentity,
        val expiresAt: Instant?,
        val auditRecorded: Boolean,
    ) : BanResult

    data object TargetUnknown : BanResult
    data object SelfTarget : BanResult
    data object TargetProtected : BanResult
    data object AlreadyBanned : BanResult
    data object DurationTooLong : BanResult
    data object RuntimeStopping : BanResult

}

internal sealed interface UnbanResult {

    data class Unbanned(
        val target: PlayerIdentity,
        val auditRecorded: Boolean,
    ) : UnbanResult

    data object TargetUnknown : UnbanResult
    data object NotBanned : UnbanResult
    data object RuntimeStopping : UnbanResult

}

internal sealed interface IpBanResult {

    data class Applied(
        val address: String,
        val expiresAt: Instant?,
        val auditRecorded: Boolean,
    ) : IpBanResult

    data object InvalidAddress : IpBanResult
    data object TargetProtected : IpBanResult
    data object AlreadyBanned : IpBanResult
    data object DurationTooLong : IpBanResult
    data object RuntimeStopping : IpBanResult

}

internal sealed interface IpUnbanResult {

    data class Unbanned(
        val address: String,
        val auditRecorded: Boolean,
    ) : IpUnbanResult

    data object InvalidAddress : IpUnbanResult
    data object NotBanned : IpUnbanResult
    data object RuntimeStopping : IpUnbanResult

}

/**
 * Domain policy for account and IP bans. Native Minecraft lists are the enforcement point; the
 * service only resolves, protects, bounds durations, commits, notifies and audits.
 */
internal class BanService(
    private val kernel: RuntimeKernel,
    private val backend: BanBackend,
    private val accountResolver: MinecraftAccountResolver,
    private val protection: TargetProtectionPolicy,
    private val audit: ModerationAuditService,
    private val settings: () -> ModerationSettings,
    private val clock: Clock,
) {

    suspend fun ban(
        actor: ModerationActor,
        rawTarget: String,
        reason: ModerationReason,
        duration: TemporaryDuration?,
    ): BanResult {
        if (isStopping()) {
            return BanResult.RuntimeStopping
        }

        val maxSeconds = settings().maxTemporaryBanSeconds
        if (duration != null
            && maxSeconds != null
            && duration.seconds > maxSeconds
        ) {
            return BanResult.DurationTooLong
        }

        val target = kernel.onServerThread { accountResolver.resolve(rawTarget) }
            ?: return BanResult.TargetUnknown

        when (kernel.onServerThread { protection.check(actor, target) }) {
            ProtectionResult.SELF -> return BanResult.SelfTarget
            ProtectionResult.PROTECTED -> return BanResult.TargetProtected
            ProtectionResult.ALLOWED -> Unit
        }

        val expiresAt = duration?.let {
            clock.instant().plusSeconds(it.seconds)
        }
        val backendResult = kernel.onServerThread {
            backend.banAccount(
                target,
                actor,
                reason,
                expiresAt
            )
        }
        if (backendResult == BanBackendResult.AlreadyBanned) {
            return BanResult.AlreadyBanned
        }

        val action = when (expiresAt) {
            null -> ModerationAuditAction.BAN
            else -> ModerationAuditAction.TEMP_BAN
        }
        val recorded = audit.record(
            actor = actor,
            action = action,
            target = target,
            reason = reason.value,
            expiresAt = expiresAt
        )
        return BanResult.Applied(
            target,
            expiresAt,
            recorded
        )
    }

    suspend fun unban(
        actor: ModerationActor,
        rawTarget: String,
    ): UnbanResult {
        if (isStopping()) {
            return UnbanResult.RuntimeStopping
        }

        val target = kernel.onServerThread { accountResolver.resolve(rawTarget) }
            ?: return UnbanResult.TargetUnknown

        if (!kernel.onServerThread { backend.unbanAccount(target) }) {
            return UnbanResult.NotBanned
        }

        val recorded = audit.record(
            actor = actor,
            action = ModerationAuditAction.UNBAN,
            target = target
        )
        return UnbanResult.Unbanned(
            target,
            recorded
        )
    }

    suspend fun banIp(
        actor: ModerationActor,
        rawTarget: String,
        reason: ModerationReason,
        duration: TemporaryDuration?,
    ): IpBanResult {
        if (isStopping()) {
            return IpBanResult.RuntimeStopping
        }

        val address = kernel.onServerThread { resolveAddress(rawTarget) }
            ?: return IpBanResult.InvalidAddress

        val maxSeconds = settings().maxTemporaryBanSeconds
        if (duration != null
            && maxSeconds != null
            && duration.seconds > maxSeconds
        ) {
            return IpBanResult.DurationTooLong
        }

        if (kernel.onServerThread { protectsOnlineAddress(actor, address) }) {
            return IpBanResult.TargetProtected
        }

        val expiresAt = duration?.let {
            clock.instant().plusSeconds(it.seconds)
        }
        val backendResult = kernel.onServerThread {
            backend.banIp(
                address,
                actor,
                reason,
                expiresAt
            )
        }
        if (backendResult == BanBackendResult.AlreadyBanned) {
            return IpBanResult.AlreadyBanned
        }

        val action = when (expiresAt) {
            null -> ModerationAuditAction.BAN_IP
            else -> ModerationAuditAction.TEMP_BAN_IP
        }
        val recorded = audit.record(
            actor = actor,
            action = action,
            target = PersistedModerationTarget.ip(address.value),
            reason = reason.value,
            expiresAt = expiresAt,
        )
        return IpBanResult.Applied(address.value, expiresAt, recorded)
    }

    suspend fun unbanIp(
        actor: ModerationActor,
        rawTarget: String,
    ): IpUnbanResult {
        if (isStopping()) return IpUnbanResult.RuntimeStopping

        val address = kernel.onServerThread { resolveAddress(rawTarget) }
            ?: return IpUnbanResult.InvalidAddress

        if (!kernel.onServerThread { backend.unbanIp(address) }) {
            return IpUnbanResult.NotBanned
        }

        val recorded = audit.record(
            actor = actor,
            action = ModerationAuditAction.UNBAN_IP,
            target = PersistedModerationTarget.ip(address.value),
        )
        return IpUnbanResult.Unbanned(
            address.value,
            recorded
        )
    }

    /** Server-thread confined; used to seed the known-identity index at startup. */
    fun listedAccountIdentities(): List<PlayerIdentity> =
        backend.listedAccountIdentities()

    private fun resolveAddress(rawTarget: String): IpAddress? {
        IpAddress.parse(rawTarget)?.let { return it }

        val online = accountResolver.resolveOnline(rawTarget)
            ?: return null
        val player = kernel.requireServer().playerList.getPlayer(online.id)
            ?: return null
        return IpAddress.parse(player.ipAddress)
    }

    private fun protectsOnlineAddress(actor: ModerationActor, address: IpAddress): Boolean {
        return !(actor !is ModerationActor.Player || !settings().protectOperators) &&
                kernel.requireServer().playerList.players.any { player ->
                    player.ipAddress == address.value &&
                            protection.check(
                                actor,
                                PlayerIdentity(
                                    player.uuid,
                                    player.gameProfile.name
                                )
                            ) == ProtectionResult.PROTECTED
                }
    }

    private fun isStopping(): Boolean {
        val state = kernel.state
        return state == KernelState.STOPPING || state == KernelState.STOPPED
    }

}
