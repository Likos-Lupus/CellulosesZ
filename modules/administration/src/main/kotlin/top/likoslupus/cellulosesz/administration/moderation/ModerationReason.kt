package top.likoslupus.cellulosesz.administration.moderation

/**
 * A validated moderation reason. Reasons are plain literal text: no formatting language, no color
 * codes, no control characters, bounded length, never blank.
 */
@JvmInline
internal value class ModerationReason private constructor(val value: String) {

    companion object {

        fun parse(
            raw: String,
            maxLength: Int,
        ): ModerationReason? {
            val value = raw.trim()
            return when {
                value.isEmpty() -> null
                value.length > maxLength -> null
                value.any(Char::isISOControl) -> null
                else -> ModerationReason(value)
            }
        }

    }

}
