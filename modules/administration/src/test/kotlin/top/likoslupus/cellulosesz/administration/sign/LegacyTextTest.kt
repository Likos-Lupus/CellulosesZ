package top.likoslupus.cellulosesz.administration.sign

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LegacyTextTest {

    @Test
    fun `codes split the text into styled segments`() {
        assertEquals(
            LegacyText.parse(
                "&cRed&r and plain",
                allowColors = true
            ),
            listOf(
                LegacySegment("Red", 'c'),
                LegacySegment(" and plain", null),
            ),
        )
    }

    @Test
    fun `the section sign is treated like an ampersand`() {
        assertEquals(
            LegacyText.parse(
                "§6Gold",
                allowColors = true
            ),
            listOf(
                LegacySegment(
                    "Gold",
                    '6'
                )
            ),
        )
    }

    @Test
    fun `colours can be stripped`() {
        assertEquals(
            LegacyText.parse(
                "&cRed",
                allowColors = false
            ),
            listOf(
                LegacySegment(
                    "&cRed",
                    null
                )
            ),
        )
    }

    @Test
    fun `plain removes every code`() {
        assertEquals(
            "Red and plain",
            LegacyText.plain("&cRed&r and plain")
        )
        assertEquals(
            "& not a code",
            LegacyText.plain("& not a code")
        )
        assertEquals(
            "Almost &",
            LegacyText.plain("Almost &")
        )
    }

}
