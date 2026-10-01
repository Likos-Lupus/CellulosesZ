package top.likoslupus.cellulosesz.movement.teleport.history

import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.util.*

/** Single-step teleport history backing `/back`. Recording the pre-teleport location swaps it. */
internal class TeleportHistoryService(private val repository: TeleportHistoryRepository) {

    private val logger = LoggerFactory.getLogger(TeleportHistoryService::class.java)

    suspend fun previous(playerId: UUID): StoredPosition? =
        try {
            repository.read(playerId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.error("teleport_history_read_failed player={}", playerId, exception)
            null
        }

    suspend fun record(playerId: UUID, position: StoredPosition) =
        repository.write(playerId, position)

}
