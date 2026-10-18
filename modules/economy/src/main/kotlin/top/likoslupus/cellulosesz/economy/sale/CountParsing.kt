package top.likoslupus.cellulosesz.economy.sale

/**
 * Parses player-supplied item counts. A bounded plain integer only: no grouping separators,
 * scientific notation or whitespace beyond a single trim. Negative counts are accepted only where a
 * `/worth` hypothetical needs them; a sale count must be positive.
 */
internal object CountParsing {

    private const val MAX_DIGITS = 9

    private val SIGNED = Regex("-?[0-9]{1,$MAX_DIGITS}")

    private val UNSIGNED = Regex("[0-9]{1,$MAX_DIGITS}")

    /** A signed count, including `0`. Null when the text is not a plain bounded integer. */
    fun signed(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || !SIGNED.matches(trimmed)) {
            return null
        }
        return trimmed.toIntOrNull()
    }

    /** A strictly positive count. Null for zero, a negative value or a malformed integer. */
    fun positive(text: String): Int? =
        signed(text)?.takeIf { it > 0 }
}
