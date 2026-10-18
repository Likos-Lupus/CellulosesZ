package top.likoslupus.cellulosesz.administration.worldtools

/**
 * Pure parser for the 8-way time values shared by `/time`, `/ptime` and `/thunder`. Returns an
 * absolute day-time in `[0, 24000)` ticks, or null for anything unrecognized.
 *
 * Accepted forms: `day`, `noon`, `night`, `midnight`, `dawn`/`sunrise`, `dusk`/`sunset`, a 24-hour
 * `HH:MM` (each hour is 1000 ticks), a 12-hour `4pm`/`4:30am`, and a raw tick count (`4000`,
 * `4000t`, `4000ticks` — normalized modulo 24000).
 */
internal object TimeValueParser {

    private const val TICKS_PER_DAY: Long = 24_000L
    private const val TICKS_PER_HOUR: Long = 1_000L

    private val NAMED: Map<String, Long> = mapOf(
        "day" to 1_000L,
        "noon" to 6_000L,
        "night" to 13_000L,
        "midnight" to 18_000L,
        "dawn" to 23_000L,
        "sunrise" to 23_000L,
        "dusk" to 12_000L,
        "sunset" to 12_000L,
    )

    private val CLOCK: Regex = Regex("(\\d{1,2}):(\\d{1,2})")
    private val TWELVE_HOUR: Regex = Regex("(\\d{1,2})(?::(\\d{1,2}))?(am|pm)")
    private val TICKS: Regex = Regex("(\\d{1,10})(t|ticks?)?")

    fun parse(raw: String): Long? {
        val text = raw.trim().lowercase()
        if (text.isEmpty()) {
            return null
        }
        NAMED[text]?.let { return it }

        CLOCK.matchEntire(text)?.let { match ->
            val hour = match.groupValues[1].toLongOrNull() ?: return null
            val minute = match.groupValues[2].toLongOrNull() ?: return null
            if (hour > 23 || minute > 59) {
                return null
            }
            return normalize(hour * TICKS_PER_HOUR + minute * TICKS_PER_HOUR / 60L)
        }

        TWELVE_HOUR.matchEntire(text)?.let { match ->
            val hour = match.groupValues[1].toLongOrNull() ?: return null
            val minute = match.groupValues[2].takeIf(String::isNotEmpty)?.toLongOrNull() ?: 0L
            val pm = match.groupValues[3] == "pm"
            if (hour !in 1..12 || minute > 59) {
                return null
            }
            val twentyFour = when {
                pm && hour != 12L -> hour + 12L
                !pm && hour == 12L -> 0L
                else -> hour
            }
            return normalize(twentyFour * TICKS_PER_HOUR + minute * TICKS_PER_HOUR / 60L)
        }

        TICKS.matchEntire(text)?.let { match ->
            val ticks = match.groupValues[1].toLongOrNull() ?: return null
            return normalize(ticks)
        }

        return null
    }

    private fun normalize(ticks: Long): Long =
        ((ticks % TICKS_PER_DAY) + TICKS_PER_DAY) % TICKS_PER_DAY

}
