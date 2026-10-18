package top.likoslupus.cellulosesz.administration.jail

/**
 * How a jail sentence's term is measured. `PERMANENT` never expires, `WALL_CLOCK` expires at a
 * fixed instant, and `ONLINE` only counts the seconds its player spends online.
 */
internal enum class JailSentenceMode {

    PERMANENT,
    WALL_CLOCK,
    ONLINE;

    companion object {

        fun parse(raw: String?): JailSentenceMode? =
            raw?.let { value ->
                entries.firstOrNull { it.name == value }
            }

    }

}

/** Pure jail online-time arithmetic. */
internal object JailSentenceMath {

    /** Remaining online seconds after [elapsedSeconds], never below zero. */
    fun decrement(remainingSeconds: Long, elapsedSeconds: Long): Long =
        (remainingSeconds - elapsedSeconds).coerceAtLeast(0L)

    /** Selects the sentence mode for a new sentence. */
    fun modeFor(durationSeconds: Long?, onlineTime: Boolean): JailSentenceMode =
        when {
            durationSeconds == null -> JailSentenceMode.PERMANENT
            onlineTime -> JailSentenceMode.ONLINE
            else -> JailSentenceMode.WALL_CLOCK
        }

}
