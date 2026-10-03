package top.likoslupus.cellulosesz.communication.messaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.communication.FakeTimeSource
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import java.util.*
import kotlin.time.Duration.Companion.seconds

class ReplyStateTest {

    private val clock = FakeTimeSource()
    private val state = ReplyState(timeout = { 60.seconds }, timeSource = clock)
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()
    private val charlie = UUID.randomUUID()

    @Test
    fun `records counterpart on both sides after delivery`() {
        state.recordSuccessfulDelivery(
            alice,
            bob,
            ReplyMode.LAST_INTERACTION
        )

        assertEquals(
            bob,
            state.resolve(
                alice,
                ReplyMode.LAST_INTERACTION
            )?.playerId
        )
        assertNull(
            state.resolve(
                alice,
                ReplyMode.LAST_INCOMING
            )
        )
        assertEquals(
            alice,
            state.resolve(
                bob,
                ReplyMode.LAST_INCOMING
            )?.playerId
        )
    }

    @Test
    fun `last incoming mode ignores the recipient's own outgoing interaction`() {
        state.recordSuccessfulDelivery(
            alice,
            bob,
            ReplyMode.LAST_INCOMING
        )
        state.recordSuccessfulDelivery(
            bob,
            charlie,
            ReplyMode.LAST_INCOMING
        )

        assertEquals(
            alice,
            state.resolve(
                bob,
                ReplyMode.LAST_INCOMING
            )?.playerId
        )
    }

    @Test
    fun `last interaction mode tracks the most recent exchange`() {
        state.recordSuccessfulDelivery(
            alice,
            bob,
            ReplyMode.LAST_INTERACTION
        )
        state.recordSuccessfulDelivery(
            bob,
            charlie,
            ReplyMode.LAST_INTERACTION
        )

        assertEquals(
            charlie,
            state.resolve(
                bob,
                ReplyMode.LAST_INTERACTION
            )?.playerId
        )
    }

    @Test
    fun `expires after the configured timeout`() {
        state.recordSuccessfulDelivery(
            alice,
            bob,
            ReplyMode.LAST_INTERACTION
        )

        clock.advance(61.seconds)

        assertNull(
            state.resolve(
                alice,
                ReplyMode.LAST_INTERACTION
            )
        )
    }

    @Test
    fun `no timeout never expires`() {
        val forever = ReplyState(
            timeout = { null },
            timeSource = clock
        )
        forever.recordSuccessfulDelivery(
            alice,
            bob,
            ReplyMode.LAST_INTERACTION
        )

        clock.advance(10_000.seconds)

        assertEquals(
            bob,
            forever.resolve(
                alice,
                ReplyMode.LAST_INTERACTION
            )?.playerId
        )
    }

    @Test
    fun `clear removes own state and dangling references`() {
        state.recordSuccessfulDelivery(
            alice,
            bob,
            ReplyMode.LAST_INTERACTION
        )
        state.recordSuccessfulDelivery(
            charlie,
            bob,
            ReplyMode.LAST_INTERACTION
        )

        state.clear(bob)

        assertNull(state.resolve(bob, ReplyMode.LAST_INTERACTION))
        assertNull(state.resolve(alice, ReplyMode.LAST_INTERACTION))
        assertNull(state.resolve(charlie, ReplyMode.LAST_INTERACTION))
    }

    @Test
    fun `unknown player has no reply target`() {
        assertNull(
            state.resolve(
                UUID.randomUUID(),
                ReplyMode.LAST_INTERACTION
            )
        )
    }

}
