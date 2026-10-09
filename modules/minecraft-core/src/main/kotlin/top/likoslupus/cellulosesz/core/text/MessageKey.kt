package top.likoslupus.cellulosesz.core.text

/**
 * A dotted translation key. Keys are owned near the feature that consumes them; there is no global
 * enum of every future message.
 */
@JvmInline
public value class MessageKey(
    public val value: String,
) {

    init {
        require(KEY_PATTERN.matches(value)) {
            "invalid message key: $value"
        }
    }

    public companion object {

        private val KEY_PATTERN = Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*")

    }

    override fun toString(): String =
        value

}
