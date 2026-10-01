package top.likoslupus.cellulosesz.movement.teleport.history

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.util.*

internal interface TeleportHistoryRepository {

    suspend fun read(playerId: UUID): StoredPosition?

    suspend fun write(playerId: UUID, position: StoredPosition)

}

/** Raised when on-disk history is unreadable or corrupt; never silently overwritten. */
internal class TeleportHistoryDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
