package top.likoslupus.cellulosesz.administration.moderation

import java.time.Duration

/**
 * Parses Minecraft-style durations into an explicit positive [TemporaryDuration]. Supported units
 * are seconds (`s`), minutes (`m`), hours (`h`), days (`d`) and weeks (`w`); units may be combined
 * (`1d12h`). Months and years are deliberately rejected because they are not fixed second counts.
 *
 * Pure Kotlin, no locale dependence, no Minecraft dependence. Returns `null` for empty, zero,
 * negative, malformed, or overflowing input.
 */
internal object DurationParser {

    private val UNIT_SECONDS: Map<Char, Long> = mapOf(
        's' to 1L,
        'm' to 60L,
        'h' to 3_600L,
        'd' to 86_400L,
        'w' to 604_800L,
    )

    fun parse(raw: String): TemporaryDuration? {
        val text = raw.trim().lowercase()
        if (text.isEmpty()) {
            return null
        }

        var index = 0
        var totalSeconds = 0L
        var parsedAny = false

        while (index < text.length) {
            val numberStart = index
            while (index < text.length && text[index].isDigit()) {
                index++
            }
            if (index == numberStart) {
                return null
            }

            val value = text.substring(numberStart, index).toLongOrNull()
                ?: return null
            if (index >= text.length) {
                return null
            }

            val unitSeconds = UNIT_SECONDS[text[index]]
                ?: return null
            index++

            totalSeconds = try {
                Math.addExact(
                    totalSeconds,
                    Math.multiplyExact(value, unitSeconds)
                )
            } catch (_: ArithmeticException) {
                return null
            }

            parsedAny = true
        }

        return when {
            !parsedAny || totalSeconds <= 0L -> null
            else -> TemporaryDuration(Duration.ofSeconds(totalSeconds))
        }
    }

}
