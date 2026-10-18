package top.likoslupus.cellulosesz.utility.unlimited

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import java.util.*
import kotlinx.coroutines.CancellationException

internal sealed interface UnlimitedListResult {

    data class Listed(
        val playerName: String,
        val items: List<String>,
    ) : UnlimitedListResult

    data object TargetOffline : UnlimitedListResult

    data object StorageUnavailable : UnlimitedListResult

}

internal sealed interface UnlimitedToggleResult {

    data class Added(
        val playerName: String,
        val itemId: String,
    ) : UnlimitedToggleResult

    data class Removed(
        val playerName: String,
        val itemId: String,
    ) : UnlimitedToggleResult

    data object NoHeldItem : UnlimitedToggleResult

    data object TargetOffline : UnlimitedToggleResult

    data object StorageUnavailable : UnlimitedToggleResult

}

internal sealed interface UnlimitedClearResult {

    data class Cleared(
        val playerName: String,
        val removed: Int,
    ) : UnlimitedClearResult

    data object TargetOffline : UnlimitedClearResult

    data object StorageUnavailable : UnlimitedClearResult

}

/**
 * Per-item unlimited sets. The set is cached in memory after startup and updated on every toggle, so
 * the per-tick top-up never touches the database.
 */
internal class UnlimitedService(
    private val runner: ServerThreadRunner,
    private val repository: UnlimitedItemRepository,
    private val backend: UnlimitedBackend,
    private val known: KnownPlayerResolver,
) {

    private val cache = HashMap<UUID, MutableSet<String>>()

    suspend fun load() {
        val loaded = try {
            repository.loadAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to load unlimited items", exception)
            return
        }
        runner.run {
            cache.clear()
            loaded.forEach { (playerId, items) ->
                cache[playerId] = LinkedHashSet(items)
            }
        }
    }

    fun shutdown() {
        cache.clear()
    }

    suspend fun list(targetId: UUID): UnlimitedListResult {
        val identity = runner.run { known.onlineById(targetId) }
            ?: return UnlimitedListResult.TargetOffline
        return try {
            UnlimitedListResult.Listed(identity.name, runner.run { sorted(targetId) })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to list unlimited items for {}", targetId, exception)
            UnlimitedListResult.StorageUnavailable
        }
    }

    suspend fun toggle(targetId: UUID): UnlimitedToggleResult {
        val identity = runner.run { known.onlineById(targetId) }
            ?: return UnlimitedToggleResult.TargetOffline
        val itemId = runner.run { backend.heldItemId(targetId) }
            ?: return UnlimitedToggleResult.NoHeldItem
        return try {
            if (repository.add(targetId, itemId)) {
                runner.run { cache.getOrPut(targetId) { LinkedHashSet() } += itemId }
                UnlimitedToggleResult.Added(identity.name, itemId)
            } else {
                repository.remove(targetId, itemId)
                runner.run { cache[targetId]?.remove(itemId) }
                UnlimitedToggleResult.Removed(identity.name, itemId)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to toggle unlimited item for {}", targetId, exception)
            UnlimitedToggleResult.StorageUnavailable
        }
    }

    suspend fun clear(targetId: UUID): UnlimitedClearResult {
        val identity = runner.run { known.onlineById(targetId) }
            ?: return UnlimitedClearResult.TargetOffline
        return try {
            val removed = repository.clear(targetId)
            runner.run { cache.remove(targetId) }
            UnlimitedClearResult.Cleared(identity.name, removed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to clear unlimited items for {}", targetId, exception)
            UnlimitedClearResult.StorageUnavailable
        }
    }

    /** Server-thread only: tops the player's unlimited items back up to `TARGET_COUNT`. */
    fun topUp(playerId: UUID) {
        val unlimited = cache[playerId]
            ?: return
        if (unlimited.isEmpty()) {
            return
        }
        backend.topUp(playerId, unlimited)
    }

    fun forget(playerId: UUID) {
        cache.remove(playerId)
    }

    /** The cached set for a player, used by the integration surface. */
    fun itemsFor(playerId: UUID): Set<String> =
        cache[playerId]?.toSet() ?: emptySet()

    private fun sorted(targetId: UUID): List<String> =
        cache[targetId]?.sorted() ?: emptyList()

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
