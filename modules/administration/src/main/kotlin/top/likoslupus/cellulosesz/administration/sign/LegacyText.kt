package top.likoslupus.cellulosesz.administration.sign

/** One run of text carrying an optional legacy formatting code (`0-9a-fk-or`). */
internal data class LegacySegment(
    val text: String,
    val code: Char?,
)

/**
 * Pure parser for `&`/`§` legacy formatting codes. When [allowColors] is false every code is
 * stripped and the whole input becomes a single plain segment. Click and hover events cannot exist
 * in a raw string, so a component rebuilt from these segments never carries one.
 */
internal object LegacyText {

    private fun isCode(char: Char): Boolean =
        char in '0'..'9'
                || char in 'a'..'f'
                || char in 'k'..'o'
                || char == 'r'

    fun parse(raw: String, allowColors: Boolean): List<LegacySegment> {
        if (!allowColors) {
            return listOf(LegacySegment(raw, null))
        }
        val segments = mutableListOf<LegacySegment>()
        val current = StringBuilder()
        var currentCode: Char? = null
        var index = 0
        while (index < raw.length) {
            val char = raw[index]
            if ((char == '&' || char == '§')
                && index + 1 < raw.length
            ) {
                val next = raw[index + 1].lowercaseChar()
                if (isCode(next)) {
                    if (current.isNotEmpty()) {
                        segments += LegacySegment(current.toString(), currentCode)
                        current.clear()
                    }
                    currentCode = if (next == 'r') null else next
                    index += 2
                    continue
                }
            }
            current.append(char)
            index++
        }
        if (current.isNotEmpty()) {
            segments += LegacySegment(current.toString(), currentCode)
        }
        return segments
    }

    /** Removes every legacy code, returning only the visible text. */
    fun plain(raw: String): String =
        buildString {
            var index = 0
            while (index < raw.length) {
                val char = raw[index]
                if ((char == '&' || char == '§')
                    && index + 1 < raw.length
                    && isCode(raw[index + 1].lowercaseChar())
                ) {
                    index += 2
                    continue
                }
                append(char)
                index++
            }
        }

}
