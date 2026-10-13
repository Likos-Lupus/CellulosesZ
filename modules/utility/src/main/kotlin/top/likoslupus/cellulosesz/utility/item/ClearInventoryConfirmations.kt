package top.likoslupus.cellulosesz.utility.item

import java.time.Instant
import java.util.*

/** One issued, not-yet-confirmed `/clearinventory` transaction. */
internal data class ClearConfirmation(
    val actorId: UUID,
    val targetId: UUID,
    val snapshot: InventorySnapshot,
)

/** The result of claiming a pending confirmation by code. */
internal sealed interface ClearClaim {

    data class Claimed(val confirmation: ClearConfirmation) : ClearClaim

    data object Invalid : ClearClaim

}

/**
 * In-memory, server-thread confined confirmation codes for `/clearinventory`. A code expires after
 * the configured window; each record carries the actor, the target and the previewed slot snapshot
 * so the confirm step can reject any change. Tracking is cleared on quit and on server stop.
 */
internal class ClearInventoryConfirmations(
    private val windowSeconds: () -> Int,
    private val clock: () -> Instant,
    private val codeGenerator: () -> String,
) {

    private data class Pending(
        val code: String,
        val expiresAt: Instant,
        val confirmation: ClearConfirmation,
    )

    private val pending = HashMap<UUID, Pending>()

    fun issue(
        actorId: UUID,
        targetId: UUID,
        snapshot: InventorySnapshot,
    ): String {
        val code = codeGenerator()
        pending[actorId] = Pending(
            code,
            clock().plusSeconds(windowSeconds().toLong()),
            ClearConfirmation(actorId, targetId, snapshot),
        )
        return code
    }

    /** Validates and consumes one actor's pending code, or returns [ClearClaim.Invalid]. */
    fun claim(actorId: UUID, code: String): ClearClaim {
        val entry = pending[actorId]
            ?: return ClearClaim.Invalid
        if (!clock().isBefore(entry.expiresAt)) {
            pending.remove(actorId)
            return ClearClaim.Invalid
        }
        if (!entry.code.equals(code, ignoreCase = true)) {
            return ClearClaim.Invalid
        }
        pending.remove(actorId)
        return ClearClaim.Claimed(entry.confirmation)
    }

    fun forget(actorId: UUID) {
        pending.remove(actorId)
    }

    fun clear() {
        pending.clear()
    }

}
