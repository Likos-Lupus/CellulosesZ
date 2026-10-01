package top.likoslupus.cellulosesz.communication.messaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

class ConversationStateTest {

    private val state = ConversationState()
    private val first = UUID.randomUUID()
    private val second = UUID.randomUUID()

    @Test
    fun `records the counterpart in both directions`() {
        state.record(first, second)

        assertEquals(second, state.partnerOf(first))
        assertEquals(first, state.partnerOf(second))
    }

    @Test
    fun `clear removes the pairing`() {
        state.record(first, second)

        state.clear(first)

        assertNull(state.partnerOf(first))
    }

    @Test
    fun `unknown player has no partner`() {
        assertNull(state.partnerOf(UUID.randomUUID()))
    }

}
