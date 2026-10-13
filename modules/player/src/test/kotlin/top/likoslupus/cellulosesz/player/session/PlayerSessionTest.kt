package top.likoslupus.cellulosesz.player.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlayerSessionTest {

    @Test
    fun `closed session duration is measured to its close time`() {
        val login = Instant.ofEpochSecond(1_000)
        val session = PlayerSession(
            sessionId = UUID.randomUUID(),
            playerId = UUID.randomUUID(),
            loginAt = login,
            lastHeartbeatAt = login,
            closedAt = login.plusSeconds(90),
            closeKind = SessionCloseKind.NORMAL,
        )

        assertEquals(
            90.seconds,
            session.durationAt(login.plusSeconds(3_600))
        )
    }

    @Test
    fun `open session duration runs up to now`() {
        val login = Instant.ofEpochSecond(1_000)
        val session = PlayerSession(
            sessionId = UUID.randomUUID(),
            playerId = UUID.randomUUID(),
            loginAt = login,
            lastHeartbeatAt = login,
            closedAt = null,
            closeKind = null,
        )

        assertEquals(
            5.minutes,
            session.durationAt(login.plusSeconds(300))
        )
    }

    @Test
    fun `duration never goes negative when the clock moves backwards`() {
        val login = Instant.ofEpochSecond(1_000)
        val session = PlayerSession(
            sessionId = UUID.randomUUID(),
            playerId = UUID.randomUUID(),
            loginAt = login,
            lastHeartbeatAt = login,
            closedAt = null,
            closeKind = null,
        )

        assertEquals(
            Duration.ZERO,
            session.durationAt(login.minusSeconds(60))
        )
    }

}
