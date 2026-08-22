package top.likoslupus.cellulosesz.core.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Path

class JacksonKotlinSupportTest {

    data class KotlinConfigFixture(
        val name: String,
        val count: Int = 3,
        val enabled: Boolean = true,
        val items: List<String> = emptyList(),
    )

    @Test
    fun jsonRoundTrip_dataClass_serializesAndDeserializes() {
        val original = KotlinConfigFixture(
            name = "test-server",
            count = 42,
            enabled = false,
            items = listOf("alpha", "beta"),
        )

        val json = JacksonCodecs.writeJsonString(original)
        val decoded = JacksonCodecs.readJson(json, KotlinConfigFixture::class.java)

        assertEquals(original, decoded)
    }

    @Test
    fun jsonDeserialization_withDefaults_usesDefaultValues() {
        val json = """{"name":"default-test"}"""
        val decoded = JacksonCodecs.readJson(json, KotlinConfigFixture::class.java)

        assertEquals("default-test", decoded.name)
        assertEquals(3, decoded.count)
        assertEquals(true, decoded.enabled)
        assertEquals(emptyList<String>(), decoded.items)
    }

    @Test
    fun yamlRoundTrip_dataClass_serializesAndDeserializesToFile(@TempDir tempDir: Path) {
        val original = KotlinConfigFixture(
            name = "yaml-test",
            count = 10,
            enabled = true,
            items = listOf("entry1", "entry2"),
        )
        val file = tempDir.resolve("config.yml")

        JacksonCodecs.writeYaml(file, original)
        val decoded = JacksonCodecs.readYaml(file, KotlinConfigFixture::class.java)

        assertEquals(original, decoded)
    }

    @Test
    fun jsonRoundTrip_dataClass_serializesAndDeserializesToFile(@TempDir tempDir: Path) {
        val original = KotlinConfigFixture(
            name = "file-test",
            count = 7,
            enabled = true,
            items = listOf("foo", "bar"),
        )
        val file = tempDir.resolve("config.json")

        JacksonCodecs.writeJson(file, original)
        val decoded = JacksonCodecs.readJson(file, KotlinConfigFixture::class.java)

        assertEquals(original, decoded)
    }

    @Test
    fun deepCopy_dataClass_preservesValues() {
        val original = KotlinConfigFixture(
            name = "deep-copy-test",
            count = 99,
            enabled = false,
            items = listOf("one", "two"),
        )

        val copied = JacksonCodecs.deepCopy(original, KotlinConfigFixture::class.java)

        assertEquals(original, copied)
    }

    @Test
    fun unknownProperties_throwsException() {
        val jsonWithUnknown = """{"name":"test","unknownField":"unexpected"}"""

        assertThrows(IOException::class.java) {
            JacksonCodecs.readJson(jsonWithUnknown, KotlinConfigFixture::class.java)
        }
    }

}
