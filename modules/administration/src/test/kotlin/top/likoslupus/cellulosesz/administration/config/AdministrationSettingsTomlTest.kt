package top.likoslupus.cellulosesz.administration.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.foundation.config.TomlConfigCodec

class AdministrationSettingsTomlTest {

    private val codec = TomlConfigCodec(AdministrationSettings.serializer())

    @Test
    fun `defaults decode without any input`() {
        val settings = codec.decode("")
        assertEquals(AdministrationSettings(), settings)
        assertTrue(settings.amusement.enabled)
    }

    @Test
    fun `the fun table maps to the amusement field`() {
        val settings = codec.decode(
            /* language=TOML */ """
            [fun]
            enabled = false
            """.trimIndent()
        )
        assertFalse(settings.amusement.enabled)
    }

    @Test
    fun `new tables decode their scalar fields`() {
        val settings = codec.decode(
            /* language=TOML */ """
            [moderation]
            loginAttackDelaySeconds = 12
            maxMuteSeconds = 3600

            [signEditing]
            allowBack = true
            maxLineLength = 64

            [entityTools]
            maxSpawnAmount = 7
            """.trimIndent()
        )
        assertEquals(12, settings.moderation.loginAttackDelaySeconds)
        assertEquals(3600L, settings.moderation.maxMuteSeconds)
        assertTrue(settings.signEditing.allowBack)
        assertEquals(64, settings.signEditing.maxLineLength)
        assertEquals(7, settings.entityTools.maxSpawnAmount)
    }

    @Test
    fun `unknown keys are rejected`() {
        val failure = runCatching {
            codec.decode(
                /* language=TOML */ """
                [moderation]
                notARealKey = 1
                """.trimIndent()
            )
        }
        assertTrue(failure.isFailure)
    }

}
