package top.likoslupus.cellulosesz.core.text.theme

import net.kyori.adventure.text.format.TextColor

/** Built-in palettes. Presets are fixed value sets, not runtime colour algorithms. */
public object TextThemePresets {

    public const val DEFAULT: String = "default"
    public const val HIGH_CONTRAST: String = "high-contrast"
    public const val MINIMAL: String = "minimal"

    public val names: List<String> = listOf(DEFAULT, HIGH_CONTRAST, MINIMAL)

    private val default = TextPalette(
        body = hex("#E8EDF4"),
        secondary = hex("#B8C4D2"),
        muted = hex("#8D9DB1"),
        brand = hex("#78AFFF"),
        heading = hex("#F5C86B"),
        value = hex("#F8DB98"),
        action = hex("#85C9FF"),
        success = hex("#86D7A0"),
        warning = hex("#FFCF79"),
        error = hex("#FF858B"),
        code = hex("#C4B3F5"),
        divider = hex("#71819A"),
    )

    private val highContrast = TextPalette(
        body = hex("#FFFFFF"),
        secondary = hex("#D6DFEA"),
        muted = hex("#AEBBCB"),
        brand = hex("#8FC1FF"),
        heading = hex("#FFD479"),
        value = hex("#FFE7A8"),
        action = hex("#9BD6FF"),
        success = hex("#9BE8B4"),
        warning = hex("#FFDA8F"),
        error = hex("#FF9A9F"),
        code = hex("#D6C8FF"),
        divider = hex("#93A3B8"),
    )

    private val minimal = TextPalette(
        body = hex("#E8EDF4"),
        secondary = hex("#B8C4D2"),
        muted = hex("#8D9DB1"),
        brand = hex("#C7D4E4"),
        heading = hex("#E8EDF4"),
        value = hex("#CFE0F5"),
        action = hex("#A9C7E8"),
        success = hex("#BFE0C9"),
        warning = hex("#E4CFA0"),
        error = hex("#E8B4B6"),
        code = hex("#CDC4E0"),
        divider = hex("#5C6A7D"),
    )

    public fun palette(name: String): TextPalette? =
        when (name) {
            DEFAULT -> default
            HIGH_CONTRAST -> highContrast
            MINIMAL -> minimal
            else -> null
        }

    public fun requirePalette(name: String): TextPalette =
        palette(name)
            ?: error("unknown text theme preset: $name")

    private fun hex(value: String): TextColor =
        TextColor.fromHexString(value)
            ?: error("invalid preset colour: $value")

}
