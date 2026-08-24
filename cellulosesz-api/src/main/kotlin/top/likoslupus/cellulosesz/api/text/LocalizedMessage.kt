package top.likoslupus.cellulosesz.api.text

import top.likoslupus.cellulosesz.api.validation.requireNonBlank

/** Platform-neutral reference to a localized message template and ordered arguments. */
@JvmRecord
public data class LocalizedMessage(
    public val key: String,
    public val arguments: MessageArguments
) {

    init {
        key.requireNonBlank { "key" }
    }

    public companion object {

        @JvmStatic
        public fun of(key: String): LocalizedMessage =
            LocalizedMessage(key, MessageArguments.empty())

        @JvmStatic
        public fun of(key: String, arguments: MessageArguments): LocalizedMessage =
            LocalizedMessage(key, arguments)

    }

}
