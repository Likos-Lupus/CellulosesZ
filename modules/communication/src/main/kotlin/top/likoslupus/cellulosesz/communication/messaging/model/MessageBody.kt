package top.likoslupus.cellulosesz.communication.messaging.model

/**
 * Validated private-message text: trimmed, non-empty, bounded length, free of ISO control
 * characters. Ordinary Unicode is preserved; user text is never interpreted as formatting.
 */
@JvmInline
internal value class MessageBody private constructor(
    val value: String,
) {

    companion object {

        fun parse(
            raw: String,
            maxLength: Int,
        ): MessageBody? {
            val value = raw.trim()
            return when {
                value.isEmpty() -> null
                value.length > maxLength -> null
                value.any(Char::isISOControl) -> null
                else -> MessageBody(value)
            }
        }

    }

}
