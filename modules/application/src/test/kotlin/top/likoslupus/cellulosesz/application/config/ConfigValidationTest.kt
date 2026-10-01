package top.likoslupus.cellulosesz.application.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.movement.config.HomeSettings
import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings

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
            CellulosesConfig(homes = HomeSettings(maxPerPlayer = -1))
        )

        assertTrue(errors.any { it.path == "homes.maxPerPlayer" })
    }

    @Test
    fun `rejects non positive teleport timeout`() {
        val errors = ConfigValidation.validate(
            CellulosesConfig(teleportRequests = TeleportRequestSettings(timeoutSeconds = 0))
        )

        assertTrue(errors.any { it.path == "teleportRequests.timeoutSeconds" })
    }

}
