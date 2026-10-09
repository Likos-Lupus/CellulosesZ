package top.likoslupus.cellulosesz.application.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.foundation.config.TomlConfigCodec

class DefaultTemplateTest {

    @Test
    fun `packaged default template decodes to the Kotlin defaults`() {
        val text = checkNotNull(javaClass.getResourceAsStream("/defaults/cellulosesz.toml")) {
            "defaults/cellulosesz.toml missing from test resources"
        }.use { it.readBytes().decodeToString() }

        val decoded = TomlConfigCodec(CellulosesConfig.serializer()).decode(text)

        assertEquals(CellulosesConfig(), decoded)
    }

}
