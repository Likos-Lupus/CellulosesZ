package top.likoslupus.cellulosesz.communication.staff

import kotlinx.coroutines.runBlocking
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.FakeTimeSource
import top.likoslupus.cellulosesz.communication.ImmediateServerThreadRunner
import top.likoslupus.cellulosesz.communication.PlayerDirectory
import top.likoslupus.cellulosesz.communication.config.HelpOpSettings
import top.likoslupus.cellulosesz.communication.messaging.OnlinePlayerIdentity
import java.util.*

class HelpOpServiceTest {

    private val directory = FakeDirectory()
    private var settings = HelpOpSettings(maxMessagesPerMinute = 2)
    private val service = HelpOpService(
        runner = ImmediateServerThreadRunner,
        directory = directory,
        settings = { settings },
        timeSource = FakeTimeSource(),
    )

    private val sender = UUID.randomUUID()

    @Test
    fun `delivers to online moderators`() =
        runBlocking {
            directory.moderators += OnlinePlayerIdentity(UUID.randomUUID(), "Admin")

            val result = service.submit(
                sender,
                "Alice",
                "please help"
            )

            assertTrue(result is HelpOpResult.Delivered)
            assertEquals(1, directory.sent.size)
        }

    @Test
    fun `logs only when no moderator is online`() =
        runBlocking {
            assertEquals(
                HelpOpResult.LoggedOnly,
                service.submit(
                    sender,
                    "Alice",
                    "hello"
                )
            )
        }

    @Test
    fun `rate limits per sender`() =
        runBlocking {
            assertTrue(
                service.submit(
                    sender,
                    "Alice",
                    "one"
                ) is HelpOpResult.LoggedOnly
            )
            assertTrue(
                service.submit(
                    sender,
                    "Alice",
                    "two"
                ) is HelpOpResult.LoggedOnly
            )
            assertEquals(
                HelpOpResult.RateLimited,
                service.submit(
                    sender,
                    "Alice",
                    "three"
                )
            )
        }

    @Test
    fun `rejects invalid and disabled`() =
        runBlocking {
            assertEquals(
                HelpOpResult.InvalidMessage,
                service.submit(
                    sender,
                    "Alice",
                    "   "
                )
            )

            settings = settings.copy(enabled = false)
            assertEquals(
                HelpOpResult.Disabled,
                service.submit(
                    sender,
                    "Alice",
                    "hi"
                )
            )
        }

    private class FakeDirectory : PlayerDirectory {

        val moderators = mutableListOf<OnlinePlayerIdentity>()
        val sent = mutableListOf<Pair<UUID, Component>>()

        override fun online(): List<OnlinePlayerIdentity> = moderators

        override fun moderators(): List<OnlinePlayerIdentity> = moderators

        override fun onlineIn(dimensionId: String): List<OnlinePlayerIdentity> =
            emptyList()

        override fun send(playerId: UUID, message: Component): Boolean {
            sent += playerId to message
            return true
        }

    }

}
