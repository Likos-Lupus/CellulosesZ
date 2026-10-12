package top.likoslupus.cellulosesz.core.text

import net.kyori.adventure.text.format.TextColor
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageRole
import top.likoslupus.cellulosesz.core.text.theme.TextRole
import top.likoslupus.cellulosesz.core.text.theme.TextTheme

/**
 * The resolved two-role view used by ordinary short messages. Ordinary templates express
 * `PRIMARY`/`SECONDARY`; `SECONDARY` is the emphasis role formerly rendered gold, mapped to the
 * palette `value` token (not the new `secondary` body tone).
 */
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

/** Maps a resolved palette onto the two-role short-message view. */
public fun TextTheme.toMessageTheme(): MessageTheme =
    MessageTheme(
        primary = palette.color(TextRole.BODY),
        secondary = palette.color(TextRole.VALUE),
    )

private val HEX_COLOR_PATTERN = Regex("#[0-9a-fA-F]{6}")

/** This phase supports exactly `#RRGGBB`. Malformed values are rejected, never silently replaced. */
public fun isValidHexColor(value: String): Boolean =
    HEX_COLOR_PATTERN.matches(value)

public fun parseHexColor(value: String): TextColor? =
    value.takeIf(HEX_COLOR_PATTERN::matches)
            ?.let(TextColor::fromHexString)
