package top.likoslupus.cellulosesz.administration.playerstate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SpeedMultiplierTest {

    @Test
    fun `values above the maximum clamp down`() {
        assertEquals(
            3.0,
            SpeedMultiplier.clamp(5.0, 0.1, 3.0)
        )
    }

    @Test
    fun `values below the minimum clamp up`() {
        assertEquals(
            0.1,
            SpeedMultiplier.clamp(0.05, 0.1, 3.0)
        )
    }

    @Test
    fun `in-range values are unchanged`() {
        assertEquals(
            2.0,
            SpeedMultiplier.clamp(2.0, 0.1, 3.0)
        )
    }

    @Test
    fun `NaN clamps to the minimum`() {
        assertEquals(
            0.1,
            SpeedMultiplier.clamp(Double.NaN, 0.1, 3.0)
        )
    }

    @Test
    fun `an inverted range is normalized`() {
        assertEquals(
            3.0,
            SpeedMultiplier.clamp(5.0, 3.0, 0.1)
        )
    }

}
