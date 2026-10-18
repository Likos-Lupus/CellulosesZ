package top.likoslupus.cellulosesz.movement.location

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.time.Instant
import java.util.*

/** A player's last known position and when it was written. */
internal data class LastLocation(
    val position: StoredPosition,
    val updatedAt: Instant,
)

/** Persists one last-known position per player; `/tpoffline` reads it when the player is gone. */
internal interface LastLocationRepository {

    suspend fun write(playerId: UUID, position: StoredPosition, updatedAt: Instant)

    suspend fun read(playerId: UUID): LastLocation?

}

internal class LastLocationDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(
    message,
    cause,
)
