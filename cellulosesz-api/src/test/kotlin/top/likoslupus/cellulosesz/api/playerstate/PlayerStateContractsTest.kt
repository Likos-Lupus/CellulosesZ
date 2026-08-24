package top.likoslupus.cellulosesz.api.playerstate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class PlayerStateContractsTest {

    @Test
    fun personalTimeSetting_sealedHierarchy() {
        val fixed = PersonalTimeSetting.Fixed(6000L)
        assertEquals(6000L, fixed.ticks)
        assertInstanceOf(PersonalTimeSetting::class.java, fixed)

        val relative = PersonalTimeSetting.Relative(1000L)
        assertEquals(1000L, relative.offset)

        val reset = PersonalTimeSetting.reset()
        assertEquals(PersonalTimeSetting.Reset, reset)
    }

    @Test
    fun personalWorldState_creation() {
        val reset = PersonalWorldState.reset()
        assertEquals(PersonalTimeSetting.Reset, reset.time)
        assertEquals(PersonalWeatherSetting.RESET, reset.weather)
    }

}
