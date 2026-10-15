package top.likoslupus.cellulosesz.communication.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MeRouteModeTest {

    @Test
    fun `replace only when the service is enabled and the override is opted in`() {
        assertEquals(
            MeRouteMode.REPLACE_VANILLA,
            meRouteMode(
                ActionMessageSettings(
                    enabled = true,
                    overrideVanillaMe = true
                )
            ),
        )
        assertEquals(
            MeRouteMode.PRESERVE_VANILLA,
            meRouteMode(
                ActionMessageSettings(
                    enabled = true,
                    overrideVanillaMe = false
                )
            ),
        )
        assertEquals(
            MeRouteMode.PRESERVE_VANILLA,
            meRouteMode(
                ActionMessageSettings(
                    enabled = false,
                    overrideVanillaMe = true
                )
            ),
        )
        assertEquals(
            MeRouteMode.PRESERVE_VANILLA,
            meRouteMode(
                ActionMessageSettings(
                    enabled = false,
                    overrideVanillaMe = false
                )
            ),
        )
    }

}
