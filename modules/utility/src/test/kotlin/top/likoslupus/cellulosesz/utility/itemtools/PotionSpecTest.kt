package top.likoslupus.cellulosesz.utility.itemtools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PotionSpecTest {

    @Test
    fun `clear and apply are recognized case-insensitively`() {
        assertEquals(PotionSpec.Clear, PotionSpec.parse("clear"))
        assertEquals(PotionSpec.Clear, PotionSpec.parse("CLEAR"))
        assertEquals(PotionSpec.Apply, PotionSpec.parse(" apply "))
    }

    @Test
    fun `effect defaults duration and amplifier`() {
        assertEquals(
            PotionSpec.parse("effect:speed"),
            PotionSpec.Effect("speed", 30, 0),
        )
    }

    @Test
    fun `effect parses explicit duration and amplifier`() {
        assertEquals(
            PotionSpec.parse("effect:minecraft:regeneration:10:2"),
            PotionSpec.Effect("minecraft:regeneration", 10, 2),
        )
    }

    @Test
    fun `effect rejects out of range values`() {
        assertEquals(PotionSpec.Invalid, PotionSpec.parse("effect:speed:0"))
        assertEquals(PotionSpec.Invalid, PotionSpec.parse("effect:speed:10:300"))
        assertEquals(PotionSpec.Invalid, PotionSpec.parse("effect:"))
    }

    @Test
    fun `unknown action is invalid`() {
        assertEquals(PotionSpec.Invalid, PotionSpec.parse("drink"))
        assertEquals(PotionSpec.Invalid, PotionSpec.parse(""))
    }

}
