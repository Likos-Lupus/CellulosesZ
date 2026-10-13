package top.likoslupus.cellulosesz.movement

import java.util.*

/** Public cause selector for cross-module admin transfers, mapped onto internal teleport causes. */
public enum class MovementTeleportCause {

    ADMIN,
    CONFINEMENT,
    CORRECTION,
    RELEASE,

}

/** Public result of a cross-module transfer. Expected failures are values, not exceptions. */
public sealed interface MovementTransferResult {

    public data object Success : MovementTransferResult

    public data object PlayerOffline : MovementTransferResult

    public data class Failed(public val reasonKey: String) : MovementTransferResult

}

/**
 * Movement-local confinement port. The composition root adapts administration's confinement query
 * onto this so movement never depends on administration.
 */
public fun interface TeleportConfinementCheck {

    public fun isConfined(playerId: UUID): Boolean

}

/**
 * Movement-local reachability port. The composition root adapts administration's visibility query
 * onto this so movement can filter teleport-request recipients without depending on administration.
 */
public fun interface RequestReachabilityPort {

    public fun canReach(senderId: UUID, targetId: UUID): Boolean

}
