package top.likoslupus.cellulosesz.communication.messaging.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MessageBodyTest {

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(
            "hello",
            MessageBody.parse("  hello  ", 32)?.value
        )
    }

    @Test
    fun `rejects empty and whitespace-only`() {
        assertNull(MessageBody.parse("", 32))
        assertNull(MessageBody.parse("   ", 32))
    }

    @Test
    fun `accepts exact max length and rejects beyond`() {
        val body = "x".repeat(8)
        assertEquals(
            body,
            MessageBody.parse(body, 8)?.value
        )
        assertNull(MessageBody.parse("x".repeat(9), 8))
    }

    @Test
    fun `rejects iso control characters`() {
        assertNull(MessageBody.parse("line\nbreak", 64))
        assertNull(MessageBody.parse("tab\there", 64))
    }

    @Test
    fun `preserves unicode and section sign`() {
        assertNotNull(MessageBody.parse("héllo 世界 🙂", 64))
        assertNotNull(MessageBody.parse("color §c red", 64))
    }

}
