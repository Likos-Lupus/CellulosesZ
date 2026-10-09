package top.likoslupus.cellulosesz.core.text

import net.kyori.adventure.text.format.TextColor
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageRole

/** The resolved, validated colors used to render messages. Raw config strings never leak here. */
public data class MessageTheme(
    public val primary: TextColor,
    public val secondary: TextColor,
) {

    public fun color(role: MessageRole): TextColor =
        when (role) {
            MessageRole.PRIMARY -> primary
            MessageRole.SECONDARY -> secondary
        }

}

private val HEX_COLOR_PATTERN = Regex("#[0-9a-fA-F]{6}")

/** This phase supports exactly `#RRGGBB`. Malformed values are rejected, never silently replaced. */
public fun isValidHexColor(value: String): Boolean =
    HEX_COLOR_PATTERN.matches(value)

public fun parseHexColor(value: String): TextColor? =
    value.takeIf(HEX_COLOR_PATTERN::matches)
            ?.let(TextColor::fromHexString)

/** Converts validated settings to the runtime theme. Callers must have validated first. */
public fun TextColorSettings.toMessageTheme(): MessageTheme =
    MessageTheme(
        primary = parseHexColor(primary)
            ?: error("unvalidated primary color: $primary"),
        secondary = parseHexColor(secondary)
            ?: error("unvalidated secondary color: $secondary"),
    )
