package top.likoslupus.cellulosesz.player.nickname

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import top.likoslupus.cellulosesz.player.config.NicknameSettings
import java.util.*

/**
 * A validated nickname: the display form plus its case-insensitive, whitespace-collapsed
 * normalization used for uniqueness and `/realname` lookup. User text is stored literally.
 *
 * The display form may carry legacy `&`/section colour codes when that is permitted; the normalized
 * form is always the stripped, code-free text so uniqueness and lookup never depend on colours.
 */
internal class Nickname private constructor(
    val display: String,
    val normalized: String,
) {

    companion object {

        private val WHITESPACE: Regex = Regex("\\s+")

        /** A single legacy colour/format code, e.g. `&a` or `§l`. */
        private val COLOR_CODE: Regex =
            Regex("[&${ChatFormatting.PREFIX_CODE}][0-9a-fk-orA-FK-OR]")

        /** Normalizes any raw input for lookup: trimmed, collapsed whitespace, lowercase. */
        fun normalize(raw: String): String =
            raw.trim()
                    .replace(WHITESPACE, " ")
                    .lowercase(Locale.ROOT)

        /** Removes every legacy colour/format code so the remainder is the visible text. */
        fun stripColors(raw: String): String =
            raw.replace(COLOR_CODE, "")

        /** True when [raw] carries at least one legacy colour/format code. */
        fun hasColors(raw: String): Boolean =
            COLOR_CODE.containsMatchIn(raw)

        fun parse(
            raw: String,
            settings: NicknameSettings,
            colorsAllowed: Boolean = false,
        ): NicknameParse {
            val display = raw.trim()
            if (display.isEmpty()) {
                return NicknameParse.Empty
            }
            val colored = hasColors(display)
            if (colored && !colorsAllowed) {
                return NicknameParse.ColorsNotAllowed
            }

            val visible = stripColors(display)
            return when {
                visible.isBlank() -> NicknameParse.Empty
                visible.length < settings.minimumVisibleLength -> NicknameParse.TooShort
                visible.length > settings.maximumVisibleLength -> NicknameParse.TooLong
                display.any(Char::isISOControl) -> NicknameParse.InvalidCharacters
                !matchesPattern(visible, settings.allowedPattern) -> NicknameParse.PatternMismatch
                isBlacklisted(visible, settings.blacklist) -> NicknameParse.Blacklisted
                !allowed(visible, settings.allowSpaces) -> NicknameParse.InvalidCharacters
                else -> NicknameParse.Valid(
                    Nickname(display, normalize(visible))
                )
            }
        }

        /** Renders the stored raw display form as a component, prepending [prefix]. */
        fun component(display: String, prefix: String): Component {
            val root: MutableComponent = Component.empty()
            if (prefix.isNotEmpty()) {
                root.append(Component.literal(prefix))
            }
            var style: Style = Style.EMPTY
            val segment = StringBuilder()
            var index = 0

            fun flush() {
                if (segment.isNotEmpty()) {
                    root.append(Component.literal(segment.toString()).withStyle(style))
                    segment.setLength(0)
                }
            }

            while (index < display.length) {
                val current = display[index]
                if ((current == '&' || current == ChatFormatting.PREFIX_CODE)
                    && index + 1 < display.length
                ) {
                    val formatting = ChatFormatting.getByCode(display[index + 1])
                    if (formatting != null) {
                        flush()
                        style = when {
                            formatting == ChatFormatting.RESET -> Style.EMPTY
                            formatting.isFormat -> style.applyFormat(formatting)
                            else -> Style.EMPTY.applyFormat(formatting)
                        }
                        index += 2
                        continue
                    }
                }
                segment.append(current)
                index++
            }
            flush()
            return root
        }

        private fun matchesPattern(
            visible: String,
            pattern: String,
        ): Boolean =
            when {
                pattern.isBlank() -> true
                else -> Regex(pattern).matches(visible)
            }

        private fun isBlacklisted(
            visible: String,
            blacklist: List<String>,
        ): Boolean {
            val lowered = visible.lowercase(Locale.ROOT)
            return blacklist.any { entry ->
                entry.isNotBlank() && lowered.contains(entry.lowercase(Locale.ROOT))
            }
        }

        private fun allowed(value: String, allowSpaces: Boolean): Boolean =
            value.all { char ->
                char.isLetterOrDigit() ||
                        char == '_' ||
                        char == '-' ||
                        char == '.' ||
                        (allowSpaces && char == ' ')
            }

    }

}

/** The outcome of parsing a nickname against the configured rules. */
internal sealed interface NicknameParse {

    data class Valid(val nickname: Nickname) : NicknameParse

    data object Empty : NicknameParse

    data object TooShort : NicknameParse

    data object TooLong : NicknameParse

    data object InvalidCharacters : NicknameParse

    data object ColorsNotAllowed : NicknameParse

    data object PatternMismatch : NicknameParse

    data object Blacklisted : NicknameParse

}
