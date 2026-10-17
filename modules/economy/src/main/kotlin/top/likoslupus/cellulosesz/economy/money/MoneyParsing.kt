package top.likoslupus.cellulosesz.economy.money

/**
 * Parses player-supplied decimal strings. Only a plain non-negative decimal with at most two
 * fractional digits is accepted: no sign, grouping separators, scientific notation, `NaN` or
 * `Infinity`, and the input length is bounded before it reaches [BigDecimal].
 */
internal object MoneyParsing {

    private const val MAX_LENGTH = 24

    private val DECIMAL = Regex("[0-9]+(?:\\.[0-9]{1,2})?")

    fun parse(input: String): Money? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) {
            return null
        }
        if (!DECIMAL.matches(trimmed)) {
            return null
        }
        return try {
            Money.parse(trimmed)
        } catch (_: NumberFormatException) {
            null
        }
    }

}
