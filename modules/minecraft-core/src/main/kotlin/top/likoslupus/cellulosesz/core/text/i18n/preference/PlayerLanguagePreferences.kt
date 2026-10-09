package top.likoslupus.cellulosesz.core.text.i18n.preference

import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory override cache. The render path reads only from here, never JDBC. Updates follow
 * write-before-publish: the repository must succeed before the cache changes, so a failed write
 * never publishes a value that is not durable.
 */
public class PlayerLanguagePreferences(
    private val repository: PlayerLanguagePreferenceRepository,
) {

    private val overrides = ConcurrentHashMap<UUID, LanguageId>()

    public fun hydrate(values: Map<UUID, LanguageId>) {
        overrides.clear()
        overrides.putAll(values)
    }

    public fun explicit(playerId: UUID): LanguageId? =
        overrides[playerId]

    public suspend fun set(playerId: UUID, language: LanguageId) {
        repository.set(playerId, language)
        overrides[playerId] = language
    }

    public suspend fun reset(playerId: UUID) {
        repository.remove(playerId)
        overrides.remove(playerId)
    }

}
