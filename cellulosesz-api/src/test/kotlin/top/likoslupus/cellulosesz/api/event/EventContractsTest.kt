package top.likoslupus.cellulosesz.api.event

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*

class EventContractsTest {

    @Test
    fun playerAttackEvent_cancellation() {
        val player = CellPlayer(UUID.randomUUID(), "Attacker")
        val target = UUID.randomUUID()
        val event = PlayerAttackEvent(player, target, "minecraft:zombie")
        assertFalse(event.cancelled())
        assertEquals(target, event.targetPlayer())

        event.cancel()
        assertTrue(event.cancelled())
    }

    @Test
    fun playerChatEvent_mutation() {
        val player = CellPlayer(UUID.randomUUID(), "Chatter")
        val event = PlayerChatEvent(player, "hello")
        assertEquals("hello", event.message())

        event.message("modified hello")
        assertEquals("modified hello", event.message())
    }

}
