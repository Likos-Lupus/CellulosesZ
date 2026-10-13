package top.likoslupus.cellulosesz.administration.playerstate

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class SpeedSessionsTest {

    private val access = FakeSpeedAccess()
    private val sessions = SpeedSessions(access)
    private val playerId = UUID.randomUUID()

    @Test
    fun `applying a multiplier then restoring returns the original base values`() {
        access.values[playerId] = SpeedSnapshot(0.5, 0.25)

        assertTrue(sessions.apply(playerId, 2.0))
        assertEquals(
            SpeedSnapshot(1.0, 0.5),
            access.values[playerId]
        )
        assertTrue(sessions.isTracked(playerId))

        sessions.restore(playerId)
        assertEquals(
            SpeedSnapshot(0.5, 0.25),
            access.values[playerId]
        )
        assertFalse(sessions.isTracked(playerId))
    }

    @Test
    fun `reapplying computes from the original snapshot, not the current value`() {
        access.values[playerId] = SpeedSnapshot(0.5, 0.25)

        sessions.apply(playerId, 2.0)
        sessions.apply(playerId, 4.0)

        assertEquals(
            SpeedSnapshot(2.0, 1.0),
            access.values[playerId]
        )
    }

    @Test
    fun `an offline player is not modified`() {
        assertFalse(sessions.apply(playerId, 2.0))
        assertFalse(sessions.isTracked(playerId))
    }

    @Test
    fun `restoreAll restores every tracked player`() {
        val other = UUID.randomUUID()
        access.values[playerId] = SpeedSnapshot(0.1, 0.05)
        access.values[other] = SpeedSnapshot(0.2, 0.1)

        sessions.apply(playerId, 2.0)
        sessions.apply(other, 2.0)
        sessions.restoreAll()

        assertEquals(
            SpeedSnapshot(0.1, 0.05),
            access.values[playerId]
        )
        assertEquals(
            SpeedSnapshot(0.2, 0.1),
            access.values[other]
        )
    }

    private class FakeSpeedAccess : SpeedAttributeAccess {

        val values = HashMap<UUID, SpeedSnapshot>()

        override fun readSpeed(playerId: UUID): SpeedSnapshot? =
            values[playerId]

        override fun writeSpeed(playerId: UUID, value: SpeedSnapshot) {
            values[playerId] = value
        }

    }

}
