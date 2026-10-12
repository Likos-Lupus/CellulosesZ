package top.likoslupus.cellulosesz.core.text.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.text.TextColorOverrides
import top.likoslupus.cellulosesz.core.text.TextSettings

class TextThemeResolverTest {

    @Test
    fun `default settings resolve to the default preset`() {
        val theme = TextThemeResolver.resolve(TextSettings())

        assertEquals(
            TextDensity.COMFORTABLE,
            theme.density
        )
        assertEquals(
            TextThemePresets.requirePalette(TextThemePresets.DEFAULT),
            theme.palette
        )
    }

    @Test
    fun `a token override refines the preset`() {
        val theme = TextThemeResolver.resolve(
            TextSettings(colors = TextColorOverrides(body = "#010203"))
        )

        assertEquals("#010203", theme.palette.body.toString())
        assertEquals(
            TextThemePresets.requirePalette(TextThemePresets.DEFAULT).heading,
            theme.palette.heading
        )
    }

    @Test
    fun `high contrast preset changes the palette`() {
        val theme = TextThemeResolver.resolve(TextSettings(theme = TextThemePresets.HIGH_CONTRAST))

        assertEquals(
            TextThemePresets.requirePalette(TextThemePresets.HIGH_CONTRAST),
            theme.palette
        )
    }

    @Test
    fun `compact density is honored`() {
        val theme = TextThemeResolver.resolve(TextSettings(density = "compact"))

        assertEquals(
            TextDensity.COMPACT,
            theme.density
        )
    }

}
