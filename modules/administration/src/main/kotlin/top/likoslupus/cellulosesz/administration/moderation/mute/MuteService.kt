package top.likoslupus.cellulosesz.administration.moderation.mute

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CancellationException
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.ModerationState
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.protection.ProtectionResult
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.time.TemporaryDuration
import java.time.Clock
import java.time.Instant
import java.util.*

internal sealed interface MuteResult {

    data class Applied(
        val target: PlayerIdentity,
        val expiresAt: Instant?,
        val auditRecorded: Boolean,
    ) : MuteResult

    data object TargetUnknown : MuteResult
    data object SelfTarget : MuteResult
    data object TargetProtected : MuteResult
    data object AlreadyMuted : MuteResult
    data object DurationTooLong : MuteResult
    data object StorageUnavailable : MuteResult
    data object RuntimeStopping : MuteResult

}

internal sealed interface UnmuteResult {

    data class Unmuted(
        val target: PlayerIdentity,
        val auditRecorded: Boolean,
    ) : UnmuteResult

    data object TargetUnknown : UnmuteResult
    data object NotMuted : UnmuteResult
    data object StorageUnavailable : UnmuteResult
    data object RuntimeStopping : UnmuteResult

}

/**
 * Owns active mute state and its persistence. Active state is a plain map confined to the server
 * thread; the repository switches to IO. A durable mute is published only after its file is
 * written, and unpublishing only happens after the file is deleted.
 */
internal class MuteService(
    private val kernel: RuntimeKernel,
    private val repository: MuteRepository,
    private val accountResolver: MinecraftAccountResolver,
    private val protection: TargetProtectionPolicy,
    private val audit: ModerationAuditService,
    private val settings: () -> ModerationSettings,
    private val clock: Clock,
) {

    private val active = HashMap<UUID, Mute>()
    private val corrupt = HashSet<UUID>()

    @Volatile private var state: ModerationState = ModerationState.LOADING

    fun state(): ModerationState = state

    /** Server-thread confined. Corrupt records are treated as unavailable, never as unmuted. */
    fun isMuted(playerId: UUID): Boolean = activeMute(playerId) != null

    fun isCorrupt(playerId: UUID): Boolean = corrupt.contains(playerId)

    /** Server-thread confined lookup with lazy expiry. */
    fun activeMute(playerId: UUID): Mute? {
        val mute = active[playerId] ?: return null
        val expiresAt = mute.expiresAt ?: return mute
        if (expiresAt.isAfter(clock.instant())) {
            return mute
        }

        active.remove(playerId)
        scheduleRemoval(playerId)
        return null
    }

    /** Server-thread confined; resolves by online name or known identity. */
    fun info(rawTarget: String): Mute? =
        when (val target = accountResolver.resolve(rawTarget)) {
            null -> null
            else -> activeMute(target.id)
        }

    fun activeMutes(): List<Mute> = active.values.toList()

    suspend fun load() {
        val result = try {
            repository.loadAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to load moderation mutes; moderation is degraded", exception)
            state = ModerationState.DEGRADED
            return
        }

        val now = clock.instant()
        val expired = result.records.filter {
            it.expiresAt?.isBefore(now) == true
        }
        val alive = result.records.filterNot {
            it.expiresAt?.isBefore(now) == true
        }

        kernel.onServerThread {
            active.clear()
            alive.forEach { active[it.playerId] = it }
            corrupt.clear()
            corrupt.addAll(result.corruptPlayerIds)
            state = when {
                result.corruptPlayerIds.isEmpty() -> ModerationState.READY
                else -> ModerationState.DEGRADED
            }
        }

        expired.forEach { scheduleRemoval(it.playerId) }
    }

    suspend fun mute(
        actor: ModerationActor,
        rawTarget: String,
        reason: ModerationReason,
        duration: TemporaryDuration?,
    ): MuteResult {
        if (state == ModerationState.STOPPING) {
            return MuteResult.RuntimeStopping
        }
        if (state == ModerationState.LOADING) {
            return MuteResult.StorageUnavailable
        }

        val maxSeconds = settings().maxTemporaryMuteSeconds
        if (duration != null
            && maxSeconds != null
            && duration.seconds > maxSeconds
        ) {
            return MuteResult.DurationTooLong
        }

        val target = kernel.onServerThread { accountResolver.resolve(rawTarget) }
            ?: return MuteResult.TargetUnknown

        when (kernel.onServerThread { protection.check(actor, target) }) {
            ProtectionResult.SELF -> return MuteResult.SelfTarget
            ProtectionResult.PROTECTED -> return MuteResult.TargetProtected
            ProtectionResult.ALLOWED -> Unit
        }

        if (kernel.onServerThread { active.containsKey(target.id) }) {
            return MuteResult.AlreadyMuted
        }
        if (kernel.onServerThread { corrupt.contains(target.id) }) {
            return MuteResult.StorageUnavailable
        }

        val now = clock.instant()
        val expiresAt = duration?.let { now.plusSeconds(it.seconds) }
        val mute = Mute(
            playerId = target.id,
            playerName = target.name,
            actor = PersistedModerationActor.of(actor),
            reason = reason.value,
            issuedAt = now,
            expiresAt = expiresAt,
        )

        val stored = try {
            repository.put(mute)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to persist mute for {}", target.id, exception)
            false
        }
        if (!stored) {
            return MuteResult.StorageUnavailable
        }

        kernel.onServerThread { active[target.id] = mute }

        val action = when (expiresAt) {
            null -> ModerationAuditAction.MUTE
            else -> ModerationAuditAction.TEMP_MUTE
        }
        val recorded = audit.record(
            actor,
            action,
            target,
            reason.value,
            expiresAt
        )
        return MuteResult.Applied(
            target,
            expiresAt,
            recorded
        )
    }

    suspend fun unmute(
        actor: ModerationActor,
        rawTarget: String,
    ): UnmuteResult {
        if (state == ModerationState.STOPPING) {
            return UnmuteResult.RuntimeStopping
        }

        val target = kernel.onServerThread { accountResolver.resolve(rawTarget) }
            ?: return UnmuteResult.TargetUnknown

        val activeRecord = kernel.onServerThread { active[target.id] }
        val unavailable = kernel.onServerThread { corrupt.contains(target.id) }
        if (activeRecord == null && !unavailable) {
            return UnmuteResult.NotMuted
        }

        val removed = try {
            repository.remove(target.id)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to delete mute for {}", target.id, exception)
            return UnmuteResult.StorageUnavailable
        }
        if (!removed) {
            return UnmuteResult.StorageUnavailable
        }

        kernel.onServerThread {
            active.remove(target.id)
            corrupt.remove(target.id)
        }

        val recorded = audit.record(
            actor,
            ModerationAuditAction.UNMUTE,
            target
        )
        return UnmuteResult.Unmuted(
            target,
            recorded
        )
    }

    fun shutdown() {
        state = ModerationState.STOPPING
    }

    private fun scheduleRemoval(playerId: UUID) {
        kernel.launch {
            try {
                repository.remove(playerId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                LOGGER.warn("failed to delete expired mute for {}", playerId, exception)
            }
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
