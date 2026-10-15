package top.likoslupus.cellulosesz.movement.teleport

import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.movement.PreparedTeleport
import top.likoslupus.cellulosesz.movement.TeleportPreflight
import top.likoslupus.cellulosesz.movement.TeleportPreflightDecision
import top.likoslupus.cellulosesz.movement.TeleportVetoReason
import java.util.*

/**
 * Applies a [TeleportPreflight] to a fully resolved destination and returns the veto reason, or null
 * to allow. The decision is synchronous and runs on the server thread. A non-cancellation exception
 * fails closed (the move is denied) so an unknown hook failure can never let a protected move
 * through; coroutine cancellation is preserved.
 */
internal object TeleportPreflightGate {

    private val LOGGER = LoggerFactory.getLogger(TeleportPreflightGate::class.java)

    fun evaluate(
        preflight: TeleportPreflight,
        playerId: UUID,
        cause: TeleportCause,
        resolved: StoredPosition,
    ): TeleportVetoReason? {
        val decision = try {
            preflight.evaluate(
                PreparedTeleport(
                    subjectId = playerId,
                    origin = cause.toOrigin(),
                    destination = resolved.toEndpoint(),
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LOGGER.error(
                "teleport_preflight_failed subject={} cause={}",
                playerId,
                cause,
                error,
            )
            return TeleportVetoReason.PLATFORM_POLICY
        }

        return when (decision) {
            TeleportPreflightDecision.Allow -> null
            is TeleportPreflightDecision.Deny -> decision.reason
        }
    }

}
