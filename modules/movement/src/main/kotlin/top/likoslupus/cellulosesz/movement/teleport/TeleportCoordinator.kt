package top.likoslupus.cellulosesz.movement.teleport

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.movement.pending.PendingTeleportService
import top.likoslupus.cellulosesz.movement.teleport.cooldown.TeleportCooldowns
import top.likoslupus.cellulosesz.movement.teleport.history.TeleportHistoryService
import kotlin.time.Duration

/**
 * The one path every CellulosesZ-initiated player move goes through. It owns the fixed transaction:
 * preflight, optional delay, late destination resolution, commit, history, cooldown, result.
 */
internal class TeleportCoordinator(
    private val backend: TeleportBackend,
    private val pending: PendingTeleportService,
    private val cooldowns: TeleportCooldowns,
    private val history: TeleportHistoryService,
    private val historyEnabled: () -> Boolean,
) {

    private val logger = LoggerFactory.getLogger(TeleportCoordinator::class.java)

    suspend fun execute(intent: TeleportIntent): TeleportOutcome {
        val origin = backend.position(intent.subjectId)
            ?: return TeleportOutcome.PlayerOffline

        if (intent.subjectId in pending) {
            return TeleportOutcome.AlreadyPending
        }

        cooldowns.remaining(intent.subjectId, intent.policy.cooldown)?.let {
            return TeleportOutcome.Cooldown(it)
        }

        if (intent.policy.delay > Duration.ZERO) {
            val signal = pending.register(intent.subjectId, origin, intent.policy)
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
                backend.position(target.playerId) ?: return TeleportOutcome.TargetOffline
        }

        return when (
            val result = backend.move(
                intent.subjectId,
                destination,
                intent.policy
            )
        ) {
            is BackendMoveResult.Success -> {
                recordHistory(intent, origin)
                cooldowns.recordSuccess(intent.subjectId)
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

}
