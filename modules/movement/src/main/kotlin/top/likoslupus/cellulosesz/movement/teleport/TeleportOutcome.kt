package top.likoslupus.cellulosesz.movement.teleport

import top.likoslupus.cellulosesz.movement.TeleportVetoReason
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation

/** Expected teleport failures are values; only invariants, IO, and storage faults throw. */
internal sealed interface TeleportOutcome {

    data class Success(val receipt: TeleportReceipt) : TeleportOutcome

    data object PlayerOffline : TeleportOutcome

    data object TargetOffline : TeleportOutcome

    data class UnknownDimension(val dimension: String) : TeleportOutcome

    data object OutsideWorldBorder : TeleportOutcome

    data object UnsafeDestination : TeleportOutcome

    data class Cooldown(val remaining: kotlin.time.Duration) : TeleportOutcome

    data object AlreadyPending : TeleportOutcome

    data object Confined : TeleportOutcome

    data class Cancelled(val reason: TeleportCancellation) : TeleportOutcome

    data object PassengerConflict : TeleportOutcome

    /** The platform refused the move, so nothing was committed. */
    data object RejectedByPlatform : TeleportOutcome

    /** A final synchronous pre-commit hook vetoed the move. */
    data class RejectedByHook(val reason: TeleportVetoReason) : TeleportOutcome

    data object RuntimeStopping : TeleportOutcome

}
