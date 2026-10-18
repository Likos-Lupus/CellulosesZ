package top.likoslupus.cellulosesz.core.preference

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** The outcome of toggling one preference. */
public sealed interface PreferenceWriteResult {

    /** The stored value changed. */
    public data class Updated(val enabled: Boolean) : PreferenceWriteResult

    /** The stored value already had this value; nothing was written. */
    public data class Unchanged(val enabled: Boolean) : PreferenceWriteResult

    /** The row could not be written; the in-memory value is left untouched. */
    public data object StorageUnavailable : PreferenceWriteResult

}

/**
 * The in-memory view of every player preference plus the one write path. Hydrated once during server
 * start and updated on each successful toggle, so a read on the server thread never touches storage.
 * An absent row falls back to the configured default, which is why [defaults] is a function: a reload
 * may change the default for a player who never expressed a preference.
 */
public class PlayerPreferenceService(
    private val repository: PlayerPreferenceRepository,
    private val defaults: () -> Map<PlayerPreference, Boolean>,
) {

    private val cache = ConcurrentHashMap<UUID, Map<PlayerPreference, Boolean>>()

    public fun hydrate(stored: Map<UUID, Map<PlayerPreference, Boolean>>) {
        cache.clear()
        stored.forEach { (playerId, values) ->
            cache[playerId] = values.toMap()
        }
    }

    /** Server-thread read; never blocks and never consults storage. */
    public fun isEnabled(playerId: UUID, preference: PlayerPreference): Boolean =
        cache[playerId]?.get(preference)
            ?: defaults()[preference]
            ?: false

    public suspend fun set(
        playerId: UUID,
        preference: PlayerPreference,
        enabled: Boolean,
    ): PreferenceWriteResult {
        val stored = cache[playerId]?.get(preference)
        if (stored != null && stored == enabled) {
            return PreferenceWriteResult.Unchanged(enabled)
        }
        return try {
            repository.save(playerId, preference, enabled)
            cache.compute(playerId) { _, existing ->
                (existing ?: emptyMap()) + (preference to enabled)
            }
            PreferenceWriteResult.Updated(enabled)
        } catch (_: PlayerPreferenceDataException) {
            PreferenceWriteResult.StorageUnavailable
        }
    }

    public fun clearPlayer(playerId: UUID) {
        cache.remove(playerId)
    }

}
