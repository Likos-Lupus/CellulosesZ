package top.likoslupus.cellulosesz.administration.jail

/** A validated jail location name. Lowercase, `[a-z0-9_-]{1,32}`, matching other stored names. */
@JvmInline
internal value class JailName private constructor(val value: String) {

    companion object {

        private val PATTERN: Regex = Regex("[a-z0-9_-]{1,32}")

        fun parse(raw: String): JailName? =
            if (PATTERN.matches(raw)) {
                JailName(raw)
            } else {
                null
            }

    }

}
