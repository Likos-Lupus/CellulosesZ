package top.likoslupus.cellulosesz.api.text

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TextContractsTest {

    @Test
    fun localizedMessage_creation() {
        val msg = LocalizedMessage.of("commands.test")
        assertEquals("commands.test", msg.key)
        assertTrue(msg.arguments.isEmpty())

        val withArgs = LocalizedMessage.of(
            "commands.test.arg",
            MessageArguments.builder()
                    .add("player1")
                    .add(42)
                    .add(true)
                    .build()
        )
        assertEquals(3, withArgs.arguments.values().size)
    }

    @Test
    fun richText_appendAndPlain() {
        val r1 = RichText.plain("Hello ")
        val r2 = RichText.plain("World")
        val combined = r1.append(r2)
        assertEquals("Hello World", combined.plainText())
        assertEquals(2, combined.segments.size)
    }

    @Test
    fun textStyle_withers() {
        val style = TextStyle.EMPTY
                .withColor("#FF0000")
                .withBold(true)
                .withItalic(true)
        assertEquals("#FF0000", style.color)
        assertTrue(style.bold)
        assertTrue(style.italic)
        assertFalse(style.underlined)
    }

}
