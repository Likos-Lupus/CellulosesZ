package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.MessageSupport
import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Maps player domain values to semantic output. It holds no language and no colour: the presentation
 * layer renders per recipient.
 *
 * The feature groups live in extensions next to this file (`PresenceMessages.kt`, `WhoisMessages.kt`,
 * `AfkMessages.kt`, `NearbyMessages.kt`, `ListMessages.kt`, `NicknameMessages.kt`); call sites keep
 * using `PlayerMessages.someResult(...)`.
 */
internal object PlayerMessages : MessageSupport() {

    fun notFound(name: String): Notice =
        error(
            MessageKey("player.player_not_found"),
            literal(name)
        )

    fun storageUnavailable(): Notice =
        error(MessageKey("player.storage_unavailable"))

    internal fun since(at: Instant, now: Instant): Duration =
        (now.toEpochMilli() - at.toEpochMilli()).coerceAtLeast(0L).milliseconds

    internal fun text(value: String): LocalizedText =
        LocalizedText(MessageKey("core.value"), listOf(playerName(value)))

    /** Formats a float without a trailing `.0`, so health reads `20` or `19.5`. */
    internal fun decimal(value: Float): String =
        when (val rounded = value.toDouble()) {
            rounded.toLong().toDouble() -> rounded.toLong().toString()
            else -> String.format(Locale.ROOT, "%.1f", rounded)
        }

    /** Vanilla food scale, so hunger reads as a fraction of twenty. */
    internal const val MAX_FOOD: Int = 20

}
