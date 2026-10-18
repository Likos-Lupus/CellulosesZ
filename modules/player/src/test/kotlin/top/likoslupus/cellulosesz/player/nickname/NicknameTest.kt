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

    @Test
    fun `strips legacy colour codes for the visible value`() {
        assertEquals("Cool", Nickname.stripColors("&aCool"))
        assertEquals("Cool", Nickname.stripColors("§lCool"))
        assertEquals(true, Nickname.hasColors("&aCool"))
        assertEquals(false, Nickname.hasColors("Cool"))
        assertEquals("Cool", Nickname.stripColors("&a&lCool"))
    }

    @Test
    fun `rejects colour codes unless they are allowed`() {
        assertEquals(
            NicknameParse.ColorsNotAllowed,
            Nickname.parse("&aCool", settings, colorsAllowed = false)
        )

        val parsed = Nickname.parse("&aCool", settings, colorsAllowed = true)
        val valid = assertInstanceOf(
            NicknameParse.Valid::class.java,
            parsed
        )
        assertEquals("&aCool", valid.nickname.display)
        assertEquals("cool", valid.nickname.normalized)
    }

    @Test
    fun `measures length on the visible text only`() {
        assertEquals(
            NicknameParse.TooShort,
            Nickname.parse("&aA", settings, colorsAllowed = true)
        )
    }

    @Test
    fun `enforces the allowed pattern on the visible text`() {
        val patterned = settings.copy(allowedPattern = "[a-z ]+")

        assertEquals(
            NicknameParse.PatternMismatch,
            Nickname.parse("Cool", patterned)
        )
        assertInstanceOf(
            NicknameParse.Valid::class.java,
            Nickname.parse("cool name", patterned)
        )
    }

    @Test
    fun `rejects blacklisted words case-insensitively`() {
        val blocked = settings.copy(blacklist = listOf("admin"))

        assertEquals(
            NicknameParse.Blacklisted,
            Nickname.parse("The Admin", blocked)
        )
    }

}
