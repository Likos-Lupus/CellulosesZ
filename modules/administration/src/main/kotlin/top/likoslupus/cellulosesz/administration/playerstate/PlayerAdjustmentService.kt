package top.likoslupus.cellulosesz.administration.playerstate

import top.likoslupus.cellulosesz.administration.config.PlayerAdjustmentSettings
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import java.util.*

internal sealed interface ExperienceResult {

    data class Viewed(
        val target: String,
        val level: Int,
        val total: Int,
        val needed: Int,
    ) : ExperienceResult

    data class Changed(
        val target: String,
        val operation: ExperienceOperation,
        val amount: Int,
        val level: Int,
        val total: Int,
    ) : ExperienceResult

    data object TargetOffline : ExperienceResult

}

internal sealed interface SpeedResult {

    data class Applied(
        val target: String,
        val multiplier: Double,
        val walk: Double,
        val fly: Double,
    ) : SpeedResult

    data object TargetOffline : SpeedResult

}

/**
 * Operator experience and speed adjustments. Experience mutations go through
 * `Player.giveExperiencePoints`; speed is session-scoped and restored from an in-memory snapshot.
 */
internal class PlayerAdjustmentService(
    private val backend: PlayerAdjustmentBackend,
    private val known: KnownPlayerResolver,
    private val settings: () -> PlayerAdjustmentSettings,
) {

    private val sessions = SpeedSessions(backend)

    fun viewExperience(targetId: UUID): ExperienceResult {
        val target = known.onlineById(targetId)?.name
            ?: return ExperienceResult.TargetOffline
        val state = backend.experience(targetId)
            ?: return ExperienceResult.TargetOffline
        return ExperienceResult.Viewed(
            target,
            state.level,
            state.total,
            state.needed
        )
    }

    fun adjustExperience(
        targetId: UUID,
        operation: ExperienceOperation,
        amount: Int,
    ): ExperienceResult {
        val target = known.onlineById(targetId)?.name
            ?: return ExperienceResult.TargetOffline
        val state = backend.experience(targetId)
            ?: return ExperienceResult.TargetOffline
        val change = ExperienceMath.resolve(
            operation = operation,
            amount = amount,
            currentTotal = state.total,
            maxAdjustment = settings().maximumExperienceAdjustment,
        )
        val after = backend.applyExperienceDelta(targetId, change.delta)
            ?: return ExperienceResult.TargetOffline
        return ExperienceResult.Changed(
            target = target,
            operation = operation,
            amount = change.applied,
            level = after.level,
            total = after.total,
        )
    }

    fun setSpeed(targetId: UUID, multiplier: Double): SpeedResult {
        val target = known.onlineById(targetId)?.name
            ?: return SpeedResult.TargetOffline
        val limits = settings()
        val capped = SpeedMultiplier.clamp(
            multiplier,
            limits.minimumSpeedMultiplier,
            limits.maximumSpeedMultiplier,
        )
        if (!sessions.apply(targetId, capped)) {
            return SpeedResult.TargetOffline
        }
        val applied = backend.readSpeed(targetId)
            ?: return SpeedResult.TargetOffline
        return SpeedResult.Applied(
            target,
            capped,
            applied.walk,
            applied.fly
        )
    }

    fun restoreSpeed(playerId: UUID) {
        sessions.restore(playerId)
    }

    fun restoreAllSpeeds() {
        sessions.restoreAll()
    }

}
