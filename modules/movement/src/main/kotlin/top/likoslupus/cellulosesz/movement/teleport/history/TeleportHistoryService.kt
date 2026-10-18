package top.likoslupus.cellulosesz.movement.teleport.history

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.util.*
import kotlinx.coroutines.CancellationException

/**
 * Single-step-per-cause teleport history backing `/back`. Recording the pre-teleport location swaps
 * the [HistoryCause.TELEPORT] row; recording a death swaps the [HistoryCause.DEATH] row. Both are
 * kept so `/back` can prefer a permitted death point and otherwise fall back to the previous
 * teleport location.
 */
internal class TeleportHistoryService(private val repository: TeleportHistoryRepository) {

    private val logger = LoggerFactory.getLogger(TeleportHistoryService::class.java)

    /** The location before the last successful (non-death) teleport, or null. */
    suspend fun previous(playerId: UUID): StoredPosition? =
        read(playerId, HistoryCause.TELEPORT)

    /** The last recorded death location, or null. */
    suspend fun death(playerId: UUID): StoredPosition? =
        read(playerId, HistoryCause.DEATH)

    suspend fun record(playerId: UUID, position: StoredPosition) =
        repository.write(playerId, HistoryCause.TELEPORT, position)

    suspend fun recordDeath(playerId: UUID, position: StoredPosition) =
        repository.write(playerId, HistoryCause.DEATH, position)

    private suspend fun read(playerId: UUID, cause: HistoryCause): StoredPosition? =
        try {
            repository.read(playerId, cause)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.error(
                "teleport_history_read_failed player={} cause={}",
                playerId,
                cause,
                exception
            )
            null
        }

}
