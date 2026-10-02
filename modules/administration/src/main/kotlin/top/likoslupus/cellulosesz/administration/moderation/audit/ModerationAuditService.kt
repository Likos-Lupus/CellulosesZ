package top.likoslupus.cellulosesz.administration.moderation.audit

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CancellationException
import top.likoslupus.cellulosesz.administration.config.ModerationSettings
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import java.time.Clock
import java.time.Instant
import java.util.*

/**
 * Best-effort structured audit. Enforcement never rolls back because an audit write failed; the
 * failure is logged with full context and surfaced to the caller as `false`.
 */
internal class ModerationAuditService(
    private val repository: ModerationAuditRepository,
    private val settings: () -> ModerationSettings,
    private val clock: Clock,
) {

    suspend fun record(
        actor: ModerationActor,
        action: ModerationAuditAction,
        target: PlayerIdentity,
        reason: String? = null,
        expiresAt: Instant? = null,
    ): Boolean =
        record(
            actor = actor,
            action = action,
            target = PersistedModerationTarget.account(target),
            reason = reason,
            expiresAt = expiresAt
        )

    suspend fun record(
        actor: ModerationActor,
        action: ModerationAuditAction,
        target: PersistedModerationTarget,
        reason: String? = null,
        expiresAt: Instant? = null,
        details: Map<String, String> = emptyMap(),
    ): Boolean {
        if (!settings().auditEnabled) return true

        val now = clock.instant()
        val entry = ModerationAuditFile(
            id = UUID.randomUUID().toString(),
            occurredAtEpochMillis = now.toEpochMilli(),
            actor = PersistedModerationActor.of(actor),
            action = action,
            target = target,
            reason = reason,
            expiresAtEpochMillis = expiresAt?.toEpochMilli(),
            details = details,
        )

        return try {
            repository.append(entry)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to write moderation audit record action={} target={} actor={}",
                action,
                target,
                PersistedModerationActor.of(actor),
                exception,
            )
            false
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
