package top.likoslupus.cellulosesz.movement.teleport

import top.likoslupus.cellulosesz.movement.TeleportVetoReason
import java.util.*

/** Backend-level result of a commit attempt. Mapped by the coordinator to a [TeleportOutcome]. */
internal sealed interface BackendMoveResult {

    data class Success(val applied: StoredPosition) : BackendMoveResult

    data object PlayerOffline : BackendMoveResult

    data class UnknownDimension(val dimension: String) : BackendMoveResult

    data object OutsideWorldBorder : BackendMoveResult

    data object UnsafeDestination : BackendMoveResult

    data object PassengerConflict : BackendMoveResult

    /**
     * The platform refused the move (a cancellable travel event, e.g. NeoForge's travel-to-dimension
     * hook) or the player object was already removed. The Minecraft teleport call reports this by
     * returning null, so no success state may be recorded.
     */
    data object RejectedByPlatform : BackendMoveResult

    /** A final synchronous pre-commit hook vetoed the move. Nothing was committed. */
    data class RejectedByHook(val reason: TeleportVetoReason) : BackendMoveResult

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
        cause: TeleportCause,
    ): BackendMoveResult

}
