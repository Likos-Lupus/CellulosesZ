package top.likoslupus.cellulosesz.movement.teleport

import java.util.*

/**
 * Where a teleport is headed. A [Player] destination is intentionally lazy: it must be re-resolved
 * at commit time so a delay never delivers the subject to a stale snapshot of the target.
 */
internal sealed interface TeleportDestination {

    data class Fixed(val position: StoredPosition) : TeleportDestination

    data class Player(val playerId: UUID) : TeleportDestination

}
