package top.likoslupus.cellulosesz.utility.kit

import java.util.*

/**
 * A validated, canonical kit identifier. User input is trimmed and lowercased before validation so
 * `/kit Starter`, `/kit STARTER` and `/kit starter` all resolve to the same stored kit; invalid
 * characters are rejected rather than sanitized so distinct inputs can never collide.
 */
@JvmInline
internal value class KitName private constructor(val value: String) {

    companion object {

        private val PATTERN = Regex("[a-z0-9_-]{1,32}")

        fun parse(raw: String): KitName? {
            val canonical = raw.trim().lowercase(Locale.ROOT)
            return canonical
                    .takeIf(PATTERN::matches)
                    ?.let(::KitName)
        }

    }

}
