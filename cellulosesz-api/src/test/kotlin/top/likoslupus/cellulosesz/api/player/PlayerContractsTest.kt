package top.likoslupus.cellulosesz.api.player

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*

class PlayerContractsTest {

    @Test
    fun cellPlayer_properties() {
        val uuid = UUID.randomUUID()
        val player = CellPlayer(uuid, "Steve")
        assertEquals(uuid, player.uuid)
        assertEquals("Steve", player.name)
    }

    @Test
    fun resolvedPlayer_onlineAndOffline() {
        val uuid = UUID.randomUUID()
        val cellPlayer = CellPlayer(uuid, "Alex")
        val online = ResolvedPlayer(
            state = ResolvedPlayerState.ONLINE,
            uuid = uuid,
            name = "Alex",
            onlinePlayer = cellPlayer,
            vanished = false
        )
        assertEquals(ResolvedPlayerState.ONLINE, online.state)
        assertEquals(uuid, online.uuid)
        assertEquals(cellPlayer, online.onlinePlayer)
        assertFalse(online.vanished)

        val unknown = ResolvedPlayer(
            state = ResolvedPlayerState.UNKNOWN,
            uuid = null,
            name = "Ghost",
            onlinePlayer = null,
            vanished = false
        )
        assertEquals(ResolvedPlayerState.UNKNOWN, unknown.state)
        assertNull(unknown.uuid)
        assertNull(unknown.onlinePlayer)
    }

}
