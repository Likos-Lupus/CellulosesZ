package top.likoslupus.cellulosesz.movement.teleport

import java.util.*

/** Proof of a committed teleport: what moved, where from, where to, and why. */
internal data class TeleportReceipt(
    val subjectId: UUID,
    val cause: TeleportCause,
    val origin: StoredPosition,
    val destination: StoredPosition,
)
