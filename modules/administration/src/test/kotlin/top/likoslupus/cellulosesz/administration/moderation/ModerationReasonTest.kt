package top.likoslupus.cellulosesz.administration.moderation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModerationReasonTest {

    @Test
    fun `accepts plain text and trims`() {
        assertEquals(
            "griefing",
            ModerationReason.parse(
                "  griefing  ",
                256
            )?.value
        )
    }

    @Test
    fun `rejects invalid input`() {
        assertNull(ModerationReason.parse("", 256))
        assertNull(ModerationReason.parse("   ", 256))
        assertNull(ModerationReason.parse("abcd", 3))
        assertNull(ModerationReason.parse("bad\u0000reason", 256))
    }

}
