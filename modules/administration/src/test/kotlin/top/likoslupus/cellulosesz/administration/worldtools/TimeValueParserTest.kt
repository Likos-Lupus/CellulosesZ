package top.likoslupus.cellulosesz.administration.worldtools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TimeValueParserTest {

    @Test
    fun `named values map to their day times`() {
        assertEquals(
            1_000L,
            TimeValueParser.parse("day")
        )
        assertEquals(
            6_000L,
            TimeValueParser.parse("noon")
        )
        assertEquals(
            13_000L,
            TimeValueParser.parse("night")
        )
        assertEquals(
            18_000L,
            TimeValueParser.parse("midnight")
        )
        assertEquals(
            23_000L,
            TimeValueParser.parse("dawn")
        )
        assertEquals(
            23_000L,
            TimeValueParser.parse("sunrise")
        )
    }

    @Test
    fun `clock times are converted with a thousand ticks per hour`() {
        assertEquals(
            17_500L,
            TimeValueParser.parse("17:30")
        )
        assertEquals(
            16_000L,
            TimeValueParser.parse("4pm")
        )
        assertEquals(
            16_500L,
            TimeValueParser.parse("4:30pm")
        )
        assertEquals(
            4_000L,
            TimeValueParser.parse("4am")
        )
        assertEquals(
            0L,
            TimeValueParser.parse("12am")
        )
    }

    @Test
    fun `raw tick counts are normalised`() {
        assertEquals(
            4_000L,
            TimeValueParser.parse("4000ticks")
        )
        assertEquals(
            4_000L,
            TimeValueParser.parse("4000t")
        )
        assertEquals(
            25_000L % 24_000L,
            TimeValueParser.parse("25000")
        )
    }

    @Test
    fun `invalid values are rejected`() {
        assertNull(TimeValueParser.parse(""))
        assertNull(TimeValueParser.parse("24:00"))
        assertNull(TimeValueParser.parse("17:99"))
        assertNull(TimeValueParser.parse("13pm"))
        assertNull(TimeValueParser.parse("dusk o'clock"))
    }

}
