package top.likoslupus.cellulosesz.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigValidationTest {

    @Test
    fun `accepts default config`() {
        assertEquals(
            ConfigValidationResult.Valid,
            ConfigValidation.validate(CellulosesConfig())
        )
    }

    @Test
    fun `rejects unsupported schema version`() {
        val result = ConfigValidation.validate(CellulosesConfig(schemaVersion = 99))

        val invalid = result as ConfigValidationResult.Invalid
        assertEquals("schemaVersion", invalid.errors.single().path)
    }

    @Test
    fun `rejects negative max homes`() {
        val result = ConfigValidation.validate(CellulosesConfig(homes = HomeConfig(maxPerPlayer = -1)))

        val invalid = result as ConfigValidationResult.Invalid
        assertTrue(invalid.errors.any { it.path == "homes.maxPerPlayer" })
    }

    @Test
    fun `rejects non positive teleport timeout`() {
        val result = ConfigValidation.validate(
            CellulosesConfig(teleportRequests = TeleportRequestConfig(timeoutSeconds = 0))
        )

        val invalid = result as ConfigValidationResult.Invalid
        assertTrue(invalid.errors.any { it.path == "teleportRequests.timeoutSeconds" })
    }

}
