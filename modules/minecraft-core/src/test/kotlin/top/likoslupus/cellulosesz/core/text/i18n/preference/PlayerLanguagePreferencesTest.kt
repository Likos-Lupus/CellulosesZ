package top.likoslupus.cellulosesz.core.text.i18n.preference

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import java.util.*

class PlayerLanguagePreferencesTest {

    private class FakeRepository : PlayerLanguagePreferenceRepository {

        val stored = mutableMapOf<UUID, LanguageId>()
        var fail: Boolean = false

        override suspend fun loadAll(): Map<UUID, LanguageId> = stored.toMap()

        override suspend fun set(playerId: UUID, language: LanguageId) {
            if (fail) error("storage down")
            stored[playerId] = language
        }

        override suspend fun remove(playerId: UUID) {
            if (fail) error("storage down")
            stored.remove(playerId)
        }

    }

    @Test
    fun `hydrate replaces the cache`() {
        val preferences = PlayerLanguagePreferences(FakeRepository())
        val player = UUID.randomUUID()

        preferences.hydrate(mapOf(player to LanguageId.ZH_CN))
        assertEquals(
            LanguageId.ZH_CN,
            preferences.explicit(player)
        )

        preferences.hydrate(emptyMap())
        assertNull(preferences.explicit(player))
    }

    @Test
    fun `set writes before publishing`() =
        runBlocking {
            val repository = FakeRepository()
            val preferences = PlayerLanguagePreferences(repository)
            val player = UUID.randomUUID()

            preferences.set(player, LanguageId.ZH_CN)
            assertEquals(
                LanguageId.ZH_CN,
                repository.stored[player]
            )
            assertEquals(
                LanguageId.ZH_CN,
                preferences.explicit(player)
            )
        }

    @Test
    fun `failed set leaves the cache untouched`() =
        runBlocking {
            val repository = FakeRepository()
            val preferences = PlayerLanguagePreferences(repository)
            val player = UUID.randomUUID()

            preferences.hydrate(mapOf(player to LanguageId.EN_US))
            repository.fail = true

            runCatching { preferences.set(player, LanguageId.ZH_CN) }

            assertEquals(
                LanguageId.EN_US,
                preferences.explicit(player)
            )
        }

    @Test
    fun `reset deletes then clears the cache`() =
        runBlocking {
            val repository = FakeRepository()
            val preferences = PlayerLanguagePreferences(repository)
            val player = UUID.randomUUID()

            preferences.hydrate(mapOf(player to LanguageId.ZH_CN))
            preferences.reset(player)

            assertNull(repository.stored[player])
            assertNull(preferences.explicit(player))
        }

    @Test
    fun `failed reset preserves the cache`() =
        runBlocking {
            val repository = FakeRepository()
            val preferences = PlayerLanguagePreferences(repository)
            val player = UUID.randomUUID()

            preferences.hydrate(mapOf(player to LanguageId.ZH_CN))
            repository.fail = true

            runCatching { preferences.reset(player) }

            assertEquals(
                LanguageId.ZH_CN,
                preferences.explicit(player)
            )
        }

}
