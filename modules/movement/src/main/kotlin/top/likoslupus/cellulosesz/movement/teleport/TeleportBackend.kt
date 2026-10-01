package top.likoslupus.cellulosesz.movement.teleport

import java.util.*

/** Backend-level result of a commit attempt. Mapped by the coordinator to a [TeleportOutcome]. */
internal sealed interface BackendMoveResult {

    data class Success(val applied: StoredPosition) : BackendMoveResult

    data object PlayerOffline : BackendMoveResult

    data class UnknownDimension(val dimension: String) : BackendMoveResult

    data object OutsideWorldBorder : BackendMoveResult

    data object UnsafeDestination : BackendMoveResult

    data object PassengerConflict : BackendMoveResult

}

/**
 * Adapter between the movement domain and mutable Minecraft world state. It is the only abstraction
 * that may invoke a player teleport; a fake implementation backs coordinator unit tests.
 */
internal interface TeleportBackend {

    suspend fun position(playerId: UUID): StoredPosition?

    suspend fun vanillaSpawnPosition(): StoredPosition

    suspend fun move(
        playerId: UUID,
        destination: StoredPosition,
        policy: TeleportPolicy,
    ): BackendMoveResult

}
