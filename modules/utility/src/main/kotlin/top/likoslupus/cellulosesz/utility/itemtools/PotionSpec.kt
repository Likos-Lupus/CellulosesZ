package top.likoslupus.cellulosesz.utility.itemtools

import java.util.*

/**
 * A parsed `/potion` request. The `effect:` form carries the effect id plus an optional duration in
 * seconds and amplifier; both default when omitted.
 */
internal sealed interface PotionSpec {

    data object Clear : PotionSpec

    data object Apply : PotionSpec

    data class Effect(
        val effect: String,
        val seconds: Int,
        val amplifier: Int,
    ) : PotionSpec

    data object Invalid : PotionSpec

    companion object {

        const val DEFAULT_SECONDS: Int = 30
        const val MAX_SECONDS: Int = 86_400
        const val MAX_AMPLIFIER: Int = 255

        private const val EFFECT_PREFIX: String = "effect:"

        fun parse(raw: String): PotionSpec {
            val trimmed = raw.trim()
            return when {
                trimmed.equals("clear", ignoreCase = true) -> Clear
                trimmed.equals("apply", ignoreCase = true) -> Apply
                trimmed.startsWith(EFFECT_PREFIX, ignoreCase = true) -> parseEffect(trimmed)
                else -> Invalid
            }
        }

        private fun parseEffect(raw: String): PotionSpec {
            val body = raw.substring(EFFECT_PREFIX.length).trim()
            if (body.isEmpty()) {
                return Invalid
            }
            val parts = body.split(':')
            // The effect id may itself contain a namespace colon, so a numeric tail is peeled from
            // the right: `...:seconds:amplifier`, then `...:seconds`, otherwise no tail.
            if (parts.size >= 3) {
                val amplifier = parts.last().toIntOrNull()
                val seconds = parts[parts.size - 2].toIntOrNull()
                if (amplifier != null && seconds != null) {
                    val id = parts.subList(0, parts.size - 2).joinToString(":")
                    if (id.isEmpty()
                        || seconds !in 1..MAX_SECONDS
                        || amplifier !in 0..MAX_AMPLIFIER
                    ) {
                        return Invalid
                    }
                    return Effect(id.lowercase(Locale.ROOT), seconds, amplifier)
                }
            }
            if (parts.size >= 2) {
                val seconds = parts.last().toIntOrNull()
                if (seconds != null) {
                    val id = parts.subList(0, parts.size - 1).joinToString(":")
                    if (id.isEmpty() || seconds !in 1..MAX_SECONDS) {
                        return Invalid
                    }
                    return Effect(id.lowercase(Locale.ROOT), seconds, 0)
                }
            }
            return Effect(body.lowercase(Locale.ROOT), DEFAULT_SECONDS, 0)
        }

    }

}
