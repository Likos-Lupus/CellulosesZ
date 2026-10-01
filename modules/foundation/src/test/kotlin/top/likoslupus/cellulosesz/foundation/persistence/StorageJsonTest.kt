package top.likoslupus.cellulosesz.foundation.persistence

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StorageJsonTest {

    @Serializable
    private data class Sample(val a: Int = 1)

    @Test
    fun `rejects unknown keys`() {
        assertThrows(SerializationException::class.java) {
            StorageJson.format.decodeFromString(
                Sample.serializer(),
                """{"a":1,"b":2}"""
            )
        }
    }

    @Test
    fun `allows comments and trailing commas`() {
        val decoded = StorageJson.format.decodeFromString(
            Sample.serializer(),
            """
            {
                // a comment
                "a": 7
            }
            """.trimIndent(),
        )

        assertEquals(7, decoded.a)
    }

}
