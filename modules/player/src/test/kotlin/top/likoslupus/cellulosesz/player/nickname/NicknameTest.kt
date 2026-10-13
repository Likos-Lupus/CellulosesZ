package top.likoslupus.cellulosesz.player.nickname

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.player.config.NicknameSettings

class NicknameTest {

    private val settings = NicknameSettings(
        enabled = true,
        minimumVisibleLength = 2,
        maximumVisibleLength = 12,
        allowSpaces = true,
    )

    @Test
    fun `normalization trims, collapses whitespace and lowercases`() {
        assertEquals(
            "cool name",
            Nickname.normalize("  Cool   Name  ")
        )
    }

    @Test
    fun `accepts a valid nickname and keeps the display form`() {
        val parsed = Nickname.parse("  Cool Name  ", settings)

        val valid = assertInstanceOf(
            NicknameParse.Valid::class.java,
            parsed
        )
        assertEquals("Cool Name", valid.nickname.display)
        assertEquals("cool name", valid.nickname.normalized)
    }

    @Test
    fun `rejects empty, too short and too long`() {
        assertEquals(
            NicknameParse.Empty,
            Nickname.parse("   ", settings)
        )
        assertEquals(
            NicknameParse.TooShort,
            Nickname.parse("a", settings)
        )
        assertEquals(
            NicknameParse.TooLong,
            Nickname.parse("x".repeat(13), settings)
        )
    }

    @Test
    fun `rejects characters that are not allowed`() {
        assertEquals(
            NicknameParse.InvalidCharacters,
            Nickname.parse("bad!", settings)
        )
        assertEquals(
            NicknameParse.InvalidCharacters,
            Nickname.parse("two\nlines", settings)
        )
    }

    @Test
    fun `rejects spaces when they are disabled`() {
        val strict = settings.copy(allowSpaces = false)

        assertEquals(
            NicknameParse.InvalidCharacters,
            Nickname.parse("two words", strict)
        )
    }

}
