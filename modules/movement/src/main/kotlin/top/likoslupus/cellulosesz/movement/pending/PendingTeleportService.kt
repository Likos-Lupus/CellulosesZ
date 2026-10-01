package top.likoslupus.cellulosesz.movement.pending

import kotlinx.coroutines.CompletableDeferred
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.TeleportPolicy
import java.util.*

/**
 * In-memory pending-teleport state. Confined to the server thread: registration, ticking, damage,
 * and disconnect all happen there, so plain maps are used with no locking.
 */
internal class PendingTeleportService {

    private val entries = HashMap<UUID, PendingTeleport>()

    fun has(playerId: UUID): Boolean = entries.containsKey(playerId)

    fun register(
        playerId: UUID,
        origin: StoredPosition,
        policy: TeleportPolicy,
    ): CompletableDeferred<TeleportCancellation> {
        val signal = CompletableDeferred<TeleportCancellation>()
        entries[playerId] = PendingTeleport(playerId, origin, policy, signal)
        return signal
    }

    fun clear(playerId: UUID) {
        entries.remove(playerId)
    }

    /** Cancels a fresh pending teleport if the player moved beyond tolerance or changed dimension. */
    fun onTick(playerId: UUID, current: StoredPosition): TeleportCancellation? {
        val pending = entries[playerId] ?: return null
        if (!pending.policy.cancelOnMove) {
            return null
        }

        val reason = when {
            current.dimension != pending.origin.dimension ->
                TeleportCancellation.DIMENSION_CHANGED

            movedBeyond(pending, current) ->
                TeleportCancellation.MOVED

            else -> null
        } ?: return null
        cancel(playerId, reason)
        return reason
    }

    /** Cancels a pending teleport if the player took damage and the policy asks us to. */
    fun onDamage(playerId: UUID): Boolean {
        val pending = entries[playerId]
            ?: return false
        return pending.policy.cancelOnDamage
                && cancel(playerId, TeleportCancellation.DAMAGED)
    }

    fun cancel(playerId: UUID, reason: TeleportCancellation): Boolean {
        val pending = entries.remove(playerId)
            ?: return false
        return pending.signal.complete(reason)
    }

    fun cancelAll(reason: TeleportCancellation) {
        entries.values.forEach { it.signal.complete(reason) }
        entries.clear()
    }

    private fun movedBeyond(
        pending: PendingTeleport,
        current: StoredPosition
    ): Boolean {
        val dx = current.x - pending.origin.x
        val dy = current.y - pending.origin.y
        val dz = current.z - pending.origin.z
        val tolerance = pending.policy.movementTolerance
        return dx * dx + dy * dy + dz * dz > tolerance * tolerance
    }

}
