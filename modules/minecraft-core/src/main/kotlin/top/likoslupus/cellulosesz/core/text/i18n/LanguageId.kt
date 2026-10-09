package top.likoslupus.cellulosesz.core.text.i18n

import java.util.*

/**
 * A canonical CellulosesZ language identifier in Minecraft `xx_yy` form. External input accepts the
 * `xx-YY` spelling and normalizes it; only the lowercase underscore form is persisted or compared.
 */
@JvmInline
public value class LanguageId private constructor(
    public val value: String,
) {

    public companion object {

        public val EN_US: LanguageId = LanguageId("en_us")
        public val ZH_CN: LanguageId = LanguageId("zh_cn")

        private val LANGUAGE_PATTERN = Regex("[a-z]{2,3}_[a-z0-9]{2,8}")

        public fun parse(input: String): LanguageId? {
            val normalized = input
                    .trim()
                    .lowercase(Locale.ROOT)
                    .replace('-', '_')

            return normalized
                    .takeIf(LANGUAGE_PATTERN::matches)
                    ?.let(::LanguageId)
        }

    }

    override fun toString(): String =
        value

}
