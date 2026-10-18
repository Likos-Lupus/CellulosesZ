package top.likoslupus.cellulosesz.movement.location

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

/**
 * In-memory last-known-position cache plus its durable backing store. The server thread records a
 * position on every player tick; the entry is persisted once, on quit, so `/tpoffline` can resolve a
 * player who has just left. A read prefers the live cache over storage.
 */
internal class LastLocationService(
    private val repository: LastLocationRepository,
    private val now: () -> Instant = Instant::now,
) {

    private val logger = LoggerFactory.getLogger(LastLocationService::class.java)
    private val cache = ConcurrentHashMap<UUID, LastLocation>()

    /** Records the latest position for [playerId]. Must run on the server thread. */
    fun remember(playerId: UUID, position: StoredPosition) {
        cache[playerId] = LastLocation(position, now())
    }

    /**
     * Writes the cached position to storage and drops it. A storage failure is logged and keeps the
     * in-memory entry so a later quit can still persist it; it never propagates into the coroutine.
     */
    suspend fun persist(playerId: UUID) {
        val cached = cache[playerId] ?: return
        try {
            repository.write(playerId, cached.position, now())
            cache.remove(playerId, cached)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.error("last_location_write_failed player={}", playerId, exception)
        }
    }

    /** The most recent known position: the live cache first, then storage. */
    suspend fun lastLocation(playerId: UUID): LastLocation? {
        cache[playerId]?.let {
            return it
        }
        return try {
            repository.read(playerId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.error("last_location_read_failed player={}", playerId, exception)
            null
        }
    }

    fun forget(playerId: UUID) {
        cache.remove(playerId)
    }

}
