package top.likoslupus.cellulosesz.administration.playerstate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExperienceMathTest {

    @Test
    fun `give clamps to the maximum adjustment`() {
        assertEquals(
            ExperienceMath.resolve(
                ExperienceOperation.GIVE,
                5000,
                0,
                1000
            ),
            ExperienceChange(1000, 1000)
        )
    }

    @Test
    fun `give below the cap is unchanged`() {
        assertEquals(
            ExperienceMath.resolve(
                ExperienceOperation.GIVE,
                200,
                50,
                1000
            ),
            ExperienceChange(200, 200)
        )
    }

    @Test
    fun `take never removes more than the current total`() {
        assertEquals(
            ExperienceMath.resolve(
                ExperienceOperation.TAKE,
                5000,
                200,
                1000
            ),
            ExperienceChange(200, -200)
        )
    }

    @Test
    fun `take is clamped to the maximum adjustment`() {
        assertEquals(
            ExperienceMath.resolve(
                ExperienceOperation.TAKE,
                5000,
                5000,
                1000
            ),
            ExperienceChange(1000, -1000)
        )
    }

    @Test
    fun `set clamps and produces the relative delta`() {
        assertEquals(
            ExperienceMath.resolve(
                ExperienceOperation.SET,
                5000,
                100,
                1000
            ),
            ExperienceChange(1000, 900)
        )
    }

    @Test
    fun `set to zero clears the total`() {
        assertEquals(
            ExperienceMath.resolve(
                ExperienceOperation.SET,
                0,
                100,
                1000
            ),
            ExperienceChange(0, -100)
        )
    }

    @Test
    fun `negative amounts clamp to zero`() {
        assertEquals(
            ExperienceMath.resolve(
                ExperienceOperation.GIVE,
                -5,
                0,
                1000
            ),
            ExperienceChange(0, 0)
        )
    }

}
