package top.likoslupus.cellulosesz.administration.moderation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DurationParserTest {

    private fun seconds(raw: String): Long? =
        DurationParser.parse(raw)?.duration?.seconds

    @Test
    fun `parses single units`() {
        assertEquals(30L, seconds("30s"))
        assertEquals(900L, seconds("15m"))
        assertEquals(7_200L, seconds("2h"))
        assertEquals(604_800L, seconds("7d"))
        assertEquals(2_419_200L, seconds("4w"))
    }

    @Test
    fun `parses combined units`() {
        assertEquals(129_600L, seconds("1d12h"))
        assertEquals(3_661L, seconds("1h1m1s"))
    }

    @Test
    fun `accepts surrounding whitespace and uppercase`() {
        assertEquals(3_600L, seconds("  1H  "))
    }

    @Test
    fun `rejects malformed input`() {
        assertNull(seconds(""))
        assertNull(seconds("0s"))
        assertNull(seconds("-1h"))
        assertNull(seconds("1month"))
        assertNull(seconds("s"))
        assertNull(seconds("1"))
        assertNull(seconds("1x"))
        assertNull(seconds("1h30"))
        assertNull(seconds("1h30x"))
    }

    @Test
    fun `rejects overflow`() {
        assertNull(seconds("999999999999999999w"))
        assertNull(seconds("99999999999999999999999999d"))
    }

}
