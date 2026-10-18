package top.likoslupus.cellulosesz.movement.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.HomeLimitSettings
import top.likoslupus.cellulosesz.movement.config.HomeSettings

class HomeLimitsTest {

    private val settings = HomeSettings(
        maxPerPlayer = 3,
        limits = listOf(
            HomeLimitSettings(permission = "group.vip", max = 5),
            HomeLimitSettings(permission = "group.admin", max = 9),
        ),
    )

    @Test
    fun `the plain default applies when no scoped permission is held`() {
        assertEquals(3, HomeLimits.maxFor(settings) { false })
    }

    @Test
    fun `the highest matching permission wins`() {
        assertEquals(9, HomeLimits.maxFor(settings) { true })
        assertEquals(5, HomeLimits.maxFor(settings) { it == "group.vip" })
    }

    @Test
    fun `a scoped permission can lower the default`() {
        val capped = HomeSettings(
            maxPerPlayer = 10,
            limits = listOf(
                HomeLimitSettings(
                    permission = "group.limited",
                    max = 2
                )
            ),
        )
        assertEquals(
            2,
            HomeLimits.maxFor(capped) { it == "group.limited" }
        )
    }

    @Test
    fun `a negative default is clamped to zero`() {
        assertEquals(
            0,
            HomeLimits.maxFor(HomeSettings(maxPerPlayer = -1)) { false }
        )
    }

}
