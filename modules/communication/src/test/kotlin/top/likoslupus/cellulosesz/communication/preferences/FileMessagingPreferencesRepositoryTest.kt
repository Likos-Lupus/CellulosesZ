package top.likoslupus.cellulosesz.communication.preferences

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class FileMessagingPreferencesRepositoryTest {

    private val root: Path = Files.createTempDirectory("cellulosesz-prefs")
    private val repository = FileMessagingPreferencesRepository { root }
    private val player = UUID.randomUUID()

    @Test
    fun `missing file loads null`() =
        runBlocking {
            assertNull(repository.load(player))
        }

    @Test
    fun `round trip preserves preferences`() =
        runBlocking {
            val ignored = UUID.randomUUID()
            val preferences = MessagingPreferences(
                receivePrivateMessages = false,
                ignoredPlayerIds = setOf(ignored),
                replyMode = ReplyMode.LAST_INCOMING,
            )

            repository.save(player, "Alice", preferences)

            assertEquals(preferences, repository.load(player))
        }

    @Test
    fun `self ignore is normalized away`() =
        runBlocking {
            repository.save(
                player,
                "Alice",
                MessagingPreferences(ignoredPlayerIds = setOf(player))
            )

            assertEquals(
                emptySet<UUID>(),
                repository.load(player)!!.ignoredPlayerIds
            )
        }

    @Test
    fun `corrupt file is not overwritten`() =
        runBlocking {
            val file = root
                    .resolve("communication")
                    .resolve("preferences")
                    .resolve("$player.json")
            Files.createDirectories(file.parent)
            Files.writeString(file, "{ not valid json")

            assertThrows(MessagingPreferencesDataException::class.java) {
                runBlocking { repository.load(player) }
            }
            assertTrue(Files.exists(file))
        }

    @Test
    fun `filename mismatch is rejected`() =
        runBlocking {
            val other = UUID.randomUUID()
            repository.save(
                other,
                "Bob",
                MessagingPreferences()
            )
            val source = root
                    .resolve("communication")
                    .resolve("preferences")
                    .resolve("$other.json")
            val target = root
                    .resolve("communication")
                    .resolve("preferences")
                    .resolve("$player.json")
            Files.copy(source, target)

            assertThrows(MessagingPreferencesDataException::class.java) {
                runBlocking { repository.load(player) }
            }
            Unit
        }

}
