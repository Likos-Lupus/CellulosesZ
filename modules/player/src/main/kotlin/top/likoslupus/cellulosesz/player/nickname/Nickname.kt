package top.likoslupus.cellulosesz.player.nickname

import top.likoslupus.cellulosesz.player.config.NicknameSettings
import java.util.*

/**
 * A validated nickname: the display form plus its case-insensitive, whitespace-collapsed
 * normalization used for uniqueness and `/realname` lookup. User text is stored literally.
 */
internal class Nickname private constructor(
    val display: String,
    val normalized: String,
) {

    companion object {

        private val WHITESPACE: Regex = Regex("\\s+")

        /** Normalizes any raw input for lookup: trimmed, collapsed whitespace, lowercase. */
        fun normalize(raw: String): String =
            raw.trim()
                    .replace(WHITESPACE, " ")
                    .lowercase(Locale.ROOT)

        fun parse(raw: String, settings: NicknameSettings): NicknameParse {
            val display = raw.trim()
            return when {
                display.isEmpty() -> NicknameParse.Empty
                display.length < settings.minimumVisibleLength -> NicknameParse.TooShort
                display.length > settings.maximumVisibleLength -> NicknameParse.TooLong
                display.any(Char::isISOControl) -> NicknameParse.InvalidCharacters
                !allowed(display, settings.allowSpaces) -> NicknameParse.InvalidCharacters
                else -> NicknameParse.Valid(Nickname(display, normalize(display)))
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

}
