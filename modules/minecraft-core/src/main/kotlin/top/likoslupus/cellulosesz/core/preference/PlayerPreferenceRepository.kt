package top.likoslupus.cellulosesz.core.preference

import java.util.*

/** A corrupt or unreadable preference row; never silently rewritten. */
public class PlayerPreferenceDataException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Persistence for the shared preference table. [loadAllBlocking] runs once during server start (the
 * initial hydration is allowed to block); every mutation is a single row upsert.
 */
public interface PlayerPreferenceRepository {

    public fun loadAllBlocking(): Map<UUID, Map<PlayerPreference, Boolean>>

    public suspend fun save(
        playerId: UUID,
        preference: PlayerPreference,
        enabled: Boolean,
    )

}
