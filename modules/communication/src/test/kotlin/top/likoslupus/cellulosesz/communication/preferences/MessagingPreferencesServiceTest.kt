package top.likoslupus.cellulosesz.communication.preferences

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.ImmediateServerThreadRunner
import top.likoslupus.cellulosesz.communication.config.PrivateMessageSettings
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import top.likoslupus.cellulosesz.communication.messaging.ReceiveDecision
import java.util.*

class MessagingPreferencesServiceTest {

    private val store = InMemoryRepository()
    private val settings = PrivateMessageSettings(maxIgnoredPlayers = 2)
    private val service = MessagingPreferencesService(
        runner = ImmediateServerThreadRunner,
        repository = store,
        settings = { settings },
    )

    private val player = UUID.randomUUID()
    private val other = UUID.randomUUID()

    @Test
    fun `missing file loads defaults`() =
        runBlocking {
            service.beginLoad(player)
            service.load(player)

            assertEquals(
                ReceiveDecision.ALLOWED,
                service.canReceiveFrom(player, other)
            )
            assertEquals(
                ReplyMode.LAST_INTERACTION,
                service.replyMode(player)
            )
        }

    @Test
    fun `corrupt preferences fail closed`() =
        runBlocking {
            store.failLoads = true
            service.beginLoad(player)
            service.load(player)

            assertEquals(
                ReceiveDecision.DENIED,
                service.canReceiveFrom(player, other)
            )
        }

    @Test
    fun `toggling receive persists and updates`() =
        runBlocking {
            service.beginLoad(player)
            service.load(player)

            val result = service.setReceivePrivateMessages(
                player,
                "Alice",
                false
            )

            assertTrue(result is PreferenceUpdateResult.Updated)
            assertEquals(
                ReceiveDecision.DENIED,
                service.canReceiveFrom(player, other)
            )
            assertFalse(store.store.getValue(player).receivePrivateMessages)
        }

    @Test
    fun `setting the same value reports already set`() =
        runBlocking {
            service.beginLoad(player)
            service.load(player)

            assertEquals(
                PreferenceUpdateResult.AlreadySet,
                service.setReceivePrivateMessages(
                    player,
                    "Alice",
                    true
                )
            )
        }

    @Test
    fun `save failure leaves runtime unchanged`() =
        runBlocking {
            service.beginLoad(player)
            service.load(player)
            store.failSaves = true

            val result = service.setReceivePrivateMessages(
                player,
                "Alice",
                false
            )

            assertEquals(PreferenceUpdateResult.StorageUnavailable, result)
            assertTrue(service.current(player)!!.receivePrivateMessages)
        }

    @Test
    fun `ignore add remove and duplicates`() =
        runBlocking {
            service.beginLoad(player)
            service.load(player)

            assertTrue(
                service.ignorePlayer(
                    player,
                    "Alice",
                    other
                ) is IgnoreUpdateResult.Updated
            )
            assertEquals(
                IgnoreUpdateResult.AlreadyIgnored,
                service.ignorePlayer(
                    player,
                    "Alice",
                    other
                )
            )
            assertEquals(
                ReceiveDecision.DENIED,
                service.canReceiveFrom(player, other)
            )
            assertTrue(
                service.unignorePlayer(
                    player,
                    "Alice",
                    other
                ) is IgnoreUpdateResult.Updated
            )
            assertEquals(
                IgnoreUpdateResult.NotIgnored,
                service.unignorePlayer(
                    player,
                    "Alice",
                    other
                )
            )
        }

    @Test
    fun `ignore list is capped`() =
        runBlocking {
            service.beginLoad(player)
            service.load(player)
            service.ignorePlayer(
                player,
                "Alice",
                UUID.randomUUID()
            )
            service.ignorePlayer(
                player,
                "Alice",
                UUID.randomUUID()
            )

            assertEquals(
                IgnoreUpdateResult.IgnoreListFull,
                service.ignorePlayer(
                    player,
                    "Alice",
                    UUID.randomUUID()
                )
            )
        }

    @Test
    fun `quit clears cached state`() =
        runBlocking {
            service.beginLoad(player)
            service.load(player)

            service.clear(player)

            assertEquals(
                ReceiveDecision.LOADING,
                service.canReceiveFrom(player, other)
            )
        }

    private class InMemoryRepository : MessagingPreferencesRepository {

        val store = HashMap<UUID, MessagingPreferences>()
        var failLoads = false
        var failSaves = false

        override suspend fun load(playerId: UUID): MessagingPreferences? {
            if (failLoads) {
                throw MessagingPreferencesDataException("boom")
            }
            return store[playerId]
        }

        override suspend fun save(
            playerId: UUID,
            playerName: String,
            preferences: MessagingPreferences,
        ) {
            if (failSaves) {
                throw RuntimeException("disk full")
            }
            store[playerId] = preferences
        }

        override suspend fun delete(playerId: UUID): Boolean =
            store.remove(playerId) != null

    }

}
