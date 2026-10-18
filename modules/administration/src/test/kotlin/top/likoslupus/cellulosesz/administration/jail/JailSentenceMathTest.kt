package top.likoslupus.cellulosesz.administration.jail

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JailSentenceMathTest {

    @Test
    fun `decrement never goes below zero`() {
        assertEquals(
            40L,
            JailSentenceMath.decrement(100L, 60L)
        )
        assertEquals(
            0L,
            JailSentenceMath.decrement(10L, 60L)
        )
        assertEquals(
            0L,
            JailSentenceMath.decrement(0L, 1L)
        )
    }

    @Test
    fun `mode selection follows the duration and the online-time toggle`() {
        assertEquals(
            JailSentenceMode.PERMANENT,
            JailSentenceMath.modeFor(null, onlineTime = true),
        )
        assertEquals(
            JailSentenceMode.ONLINE,
            JailSentenceMath.modeFor(600L, onlineTime = true),
        )
        assertEquals(
            JailSentenceMode.WALL_CLOCK,
            JailSentenceMath.modeFor(600L, onlineTime = false),
        )
    }

}
