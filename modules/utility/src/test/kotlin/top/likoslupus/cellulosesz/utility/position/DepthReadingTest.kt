package top.likoslupus.cellulosesz.utility.position

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DepthReadingTest {

    @Test
    fun `below the anchor reports depth`() {
        val reading = DepthReading(feetY = 10, minY = -64, maxY = 319)

        assertEquals(63, reading.anchor)
        assertEquals(53, reading.belowAnchor)
        assertEquals(0, reading.aboveAnchor)
    }

    @Test
    fun `above the anchor reports height`() {
        val reading = DepthReading(feetY = 100, minY = -64, maxY = 319)

        assertEquals(0, reading.belowAnchor)
        assertEquals(37, reading.aboveAnchor)
    }

    @Test
    fun `anchor is clamped into the build range`() {
        assertEquals(
            64,
            DepthReading(feetY = 0, minY = 64, maxY = 200).anchor
        )
        assertEquals(
            100,
            DepthReading(feetY = 0, minY = 100, maxY = 200).anchor
        )
    }

}
