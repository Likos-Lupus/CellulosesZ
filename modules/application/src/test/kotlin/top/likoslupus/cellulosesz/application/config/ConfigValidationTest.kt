package top.likoslupus.cellulosesz.application.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.*

class ConfigValidationTest {

    @Test
    fun `accepts default config`() {
        assertTrue(ConfigValidation.validate(CellulosesConfig()).isEmpty())
    }

    @Test
    fun `rejects unsupported schema version`() {
        val errors = ConfigValidation.validate(CellulosesConfig(schemaVersion = 99))

        assertEquals("schemaVersion", errors.single().path)
    }

    @Test
    fun `rejects negative max homes`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(movement = MovementSettings(homes = HomeSettings(maxPerPlayer = -1)))
        )

        assertTrue(errors.any { it.path == "movement.homes.maxPerPlayer" })
    }

    @Test
    fun `rejects non positive teleport timeout`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                movement = MovementSettings(
                    requests = TeleportRequestSettings(timeoutSeconds = 0)
                )
            )
        )

        assertTrue(errors.any { it.path == "movement.requests.timeoutSeconds" })
    }

    @Test
    fun `rejects out of range safety radius`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                movement = MovementSettings(
                    teleport = TeleportSettings(
                        safety = TeleportSafetySettings(searchHorizontalRadius = 999)
                    )
                )
            )
        )

        assertTrue(errors.any { it.path == "movement.teleport.safety.searchHorizontalRadius" })
    }

    @Test
    fun `rejects negative teleport delay`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(
                movement = MovementSettings(
                    teleport = TeleportSettings(
                        delaySeconds = -1
                    )
                )
            )
        )

        assertTrue(errors.any { it.path == "movement.teleport.delaySeconds" })
    }

}
