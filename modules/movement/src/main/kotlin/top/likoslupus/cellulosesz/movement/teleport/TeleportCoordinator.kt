package top.likoslupus.cellulosesz.movement.teleport

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.core.player.TemporaryProtectionRegistry
import top.likoslupus.cellulosesz.movement.CommittedTeleport
import top.likoslupus.cellulosesz.movement.TeleportCommittedObserver
import top.likoslupus.cellulosesz.movement.pending.PendingTeleportService
import top.likoslupus.cellulosesz.movement.teleport.cooldown.TeleportCooldowns
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The one path every CellulosesZ-initiated player move goes through. It owns the fixed transaction:
 * preflight, optional delay, late destination resolution, commit, history, cooldown, result, and the
 * post-commit observation of a confirmed success.
 */
internal class TeleportCoordinator(
    private val backend: TeleportBackend,
    private val pending: PendingTeleportService,
    private val cooldowns: TeleportCooldowns,
    private val history: TeleportHistoryService,
    private val historyEnabled: () -> Boolean,
    private val confinement: (UUID) -> Boolean = { false },
    private val observer: TeleportCommittedObserver = TeleportCommittedObserver.None,
    private val worldAccess: TeleportWorldAccess = TeleportWorldAccess.AllowAll,
    private val invulnerability: () -> Duration = { Duration.ZERO },
) {

    private val logger = LoggerFactory.getLogger(TeleportCoordinator::class.java)

    suspend fun execute(intent: TeleportIntent): TeleportOutcome {
        val origin = backend.position(intent.subjectId)
            ?: return TeleportOutcome.PlayerOffline

        if (intent.subjectId in pending) {
            return TeleportOutcome.AlreadyPending
        }

        // Administrative and jail transfers bypass the confinement gate so a jailed player can be
        // moved in and released; every player-instigated move is denied while confined.
        if (!intent.cause.bypassesConfinementGate
            && confinement(intent.subjectId)
        ) {
            return TeleportOutcome.Confined
        }

        cooldowns.remaining(
            intent.subjectId,
            intent.policy.cooldown
        )?.let {
            return TeleportOutcome.Cooldown(it)
        }

        if (intent.policy.delay > Duration.ZERO) {
            val signal = pending.register(
                intent.subjectId,
                origin,
                intent.policy
            )
            val cancellation = try {
                withTimeoutOrNull(intent.policy.delay) { signal.await() }
            } finally {
                pending.clear(intent.subjectId)
            }

            if (cancellation != null) {
                return TeleportOutcome.Cancelled(cancellation)
            }
        }

        val destination = when (val target = intent.destination) {
            is TeleportDestination.Fixed ->
                target.position

            is TeleportDestination.Player ->
                backend.position(target.playerId)
                    ?: return TeleportOutcome.TargetOffline
        }

        // The per-world access gate runs for every intent, on the fully resolved destination, so a
        // delayed or player-following teleport is checked against the world actually being entered.
        if (destination.dimension != origin.dimension
            && !worldAccess.allows(intent.subjectId, destination.dimension)
        ) {
            return TeleportOutcome.WorldAccessDenied(destination.dimension)
        }

        return when (
            val result = backend.move(
                intent.subjectId,
                destination,
                intent.policy,
                intent.cause,
            )
        ) {
            is BackendMoveResult.Success -> {
                recordHistory(intent, origin)
                cooldowns.recordSuccess(intent.subjectId)
                grantInvulnerability(intent.subjectId)
                observe(intent, origin, result.applied)
                TeleportOutcome.Success(
                    TeleportReceipt(
                        intent.subjectId,
                        intent.cause,
                        origin,
                        result.applied
                    )
                )
            }

            BackendMoveResult.PlayerOffline ->
                TeleportOutcome.PlayerOffline

            is BackendMoveResult.UnknownDimension ->
                TeleportOutcome.UnknownDimension(result.dimension)

            BackendMoveResult.OutsideWorldBorder ->
                TeleportOutcome.OutsideWorldBorder

            BackendMoveResult.UnsafeDestination ->
                TeleportOutcome.UnsafeDestination

            BackendMoveResult.PassengerConflict ->
                TeleportOutcome.PassengerConflict

            BackendMoveResult.RejectedByPlatform ->
                TeleportOutcome.RejectedByPlatform

            is BackendMoveResult.RejectedByHook ->
                TeleportOutcome.RejectedByHook(result.reason)
        }
    }

    private suspend fun recordHistory(intent: TeleportIntent, origin: StoredPosition) {
        if (!intent.policy.recordHistory || !historyEnabled()) {
            return
        }

        try {
            history.record(intent.subjectId, origin)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // The teleport already happened; a history write failure only costs `/back`.
            logger.error(
                "teleport_history_write_failed player={} cause={}",
                intent.subjectId,
                intent.cause,
                exception,
            )
        }
    }

    private fun grantInvulnerability(playerId: UUID) {
        val duration = invulnerability()
        if (duration <= Duration.ZERO) {
            return
        }
        TemporaryProtectionRegistry.protect(
            playerId,
            Instant.now().plusMillis(duration.inWholeMilliseconds),
        )
    }

    private fun observe(
        intent: TeleportIntent,
        origin: StoredPosition,
        applied: StoredPosition,
    ) {
        try {
            observer.onCommitted(
                CommittedTeleport(
                    subjectId = intent.subjectId,
                    origin = intent.cause.toOrigin(),
                    from = origin.toEndpoint(),
                    to = applied.toEndpoint(),
                )
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // The teleport is already committed; an observer failure never reverses that fact.
            logger.error(
                "teleport_observer_failed subject={} cause={}",
                intent.subjectId,
                intent.cause,
                exception,
            )
        }
    }

}
