package top.likoslupus.cellulosesz.movement.pending

import kotlinx.coroutines.CompletableDeferred
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.TeleportPolicy
import java.util.*

/** A teleport waiting out its delay. At most one exists per player. */
internal data class PendingTeleport(
    val playerId: UUID,
    val origin: StoredPosition,
    val policy: TeleportPolicy,
    val signal: CompletableDeferred<TeleportCancellation>,
)
