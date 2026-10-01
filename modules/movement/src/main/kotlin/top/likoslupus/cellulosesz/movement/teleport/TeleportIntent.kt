package top.likoslupus.cellulosesz.movement.teleport

import java.util.*

/** An immutable request to move one player. The single input to [TeleportCoordinator]. */
internal data class TeleportIntent(
    val subjectId: UUID,
    val destination: TeleportDestination,
    val cause: TeleportCause,
    val policy: TeleportPolicy,
)
