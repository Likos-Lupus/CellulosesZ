package top.likoslupus.cellulosesz.config

import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConfigCodecTest {

    @Test
    fun `encodes defaults`() {
        val encoded = ConfigCodec.encode(CellulosesConfig())

        assertTrue(encoded.contains("\"schemaVersion\""))
        assertTrue(encoded.contains("\"homes\""))
        assertTrue(encoded.contains("\"maxPerPlayer\""))
    }

    @Test
    fun `round trips a config`() {
        val original = CellulosesConfig(
            diagnostics = DiagnosticsConfig(verboseLogging = true),
            homes = HomeConfig(
                maxPerPlayer = 3,
                defaultName = "base"
            ),
        )

        val decoded = ConfigCodec.decode(ConfigCodec.encode(original))

        assertEquals(original, decoded)
    }

    @Test
    fun `accepts comments and trailing commas`() {
        // language=JSON5
        val text = """
            {
                // schema is mandatory
                "schemaVersion": 1,
                "homes": {
                    "maxPerPlayer": 2,
                },
            }
        """.trimIndent()

        assertEquals(2, ConfigCodec.decode(text).homes.maxPerPlayer)
    }

    @Test
    fun `rejects unknown keys`() {
        // language=JSON5
        val text = """{ "teleprot": { } }"""

        assertThrows(SerializationException::class.java) {
            ConfigCodec.decode(text)
        }
    }

}
