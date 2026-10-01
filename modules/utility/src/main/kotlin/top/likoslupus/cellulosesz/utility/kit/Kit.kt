package top.likoslupus.cellulosesz.utility.kit

@JvmInline
internal value class KitName private constructor(val value: String) {

    companion object {

        private val PATTERN = Regex("[a-z0-9_-]{1,32}")

        fun parse(raw: String): KitName? =
            if (PATTERN.matches(raw)) {
                KitName(raw)
            } else {
                null
            }

    }

}
