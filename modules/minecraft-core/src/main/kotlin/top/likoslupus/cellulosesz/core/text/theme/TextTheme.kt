package top.likoslupus.cellulosesz.core.text.theme

import net.kyori.adventure.text.format.TextColor

/** Semantic colour roles. Translations and presenters refer to roles, never to RGB values. */
public enum class TextRole {

    BODY,
    SECONDARY,
    MUTED,
    BRAND,
    HEADING,
    VALUE,
    ACTION,
    SUCCESS,
    WARNING,
    ERROR,
    CODE,
    DIVIDER,

}

/** Vertical density; it affects layout spacing only, never semantics, permissions or wording. */
public enum class TextDensity {

    COMFORTABLE,
    COMPACT,

}

/** A complete, resolved palette. Raw configuration strings never reach this type. */
public data class TextPalette(
    public val body: TextColor,
    public val secondary: TextColor,
    public val muted: TextColor,
    public val brand: TextColor,
    public val heading: TextColor,
    public val value: TextColor,
    public val action: TextColor,
    public val success: TextColor,
    public val warning: TextColor,
    public val error: TextColor,
    public val code: TextColor,
    public val divider: TextColor,
) {

    public fun color(role: TextRole): TextColor =
        when (role) {
            TextRole.BODY -> body
            TextRole.SECONDARY -> secondary
            TextRole.MUTED -> muted
            TextRole.BRAND -> brand
            TextRole.HEADING -> heading
            TextRole.VALUE -> value
            TextRole.ACTION -> action
            TextRole.SUCCESS -> success
            TextRole.WARNING -> warning
            TextRole.ERROR -> error
            TextRole.CODE -> code
            TextRole.DIVIDER -> divider
        }

}

/** An immutable theme snapshot used for one render pass. */
public data class TextTheme(
    public val palette: TextPalette,
    public val density: TextDensity,
)
