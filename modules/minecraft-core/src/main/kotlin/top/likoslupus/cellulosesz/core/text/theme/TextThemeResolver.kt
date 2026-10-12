package top.likoslupus.cellulosesz.core.text.theme

import net.kyori.adventure.text.format.TextColor
import top.likoslupus.cellulosesz.core.text.TextSettings
import top.likoslupus.cellulosesz.core.text.parseHexColor

/** Resolves validated [TextSettings] into an immutable [TextTheme]. */
public object TextThemeResolver {

    public fun resolve(settings: TextSettings): TextTheme =
        TextTheme(
            palette = resolvePalette(settings),
            density = resolveDensity(settings.density),
        )

    private fun resolvePalette(settings: TextSettings): TextPalette {
        val overrides = settings.colors
        val base = TextThemePresets.requirePalette(settings.theme)
        return base.copy(
            body = overrides.body?.let(::requireColor) ?: base.body,
            secondary = overrides.secondary?.let(::requireColor) ?: base.secondary,
            muted = overrides.muted?.let(::requireColor) ?: base.muted,
            brand = overrides.brand?.let(::requireColor) ?: base.brand,
            heading = overrides.heading?.let(::requireColor) ?: base.heading,
            value = overrides.value?.let(::requireColor) ?: base.value,
            action = overrides.action?.let(::requireColor) ?: base.action,
            success = overrides.success?.let(::requireColor) ?: base.success,
            warning = overrides.warning?.let(::requireColor) ?: base.warning,
            error = overrides.error?.let(::requireColor) ?: base.error,
            code = overrides.code?.let(::requireColor) ?: base.code,
            divider = overrides.divider?.let(::requireColor) ?: base.divider,
        )
    }

    private fun resolveDensity(value: String): TextDensity =
        when (value) {
            "compact" -> TextDensity.COMPACT
            else -> TextDensity.COMFORTABLE
        }

    private fun requireColor(value: String?): TextColor =
        value?.let(::parseHexColor)
            ?: error("unvalidated text colour: $value")

}
