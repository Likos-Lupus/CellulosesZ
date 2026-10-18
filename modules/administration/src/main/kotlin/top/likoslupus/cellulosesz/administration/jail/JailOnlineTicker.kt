package top.likoslupus.cellulosesz.administration.jail

import com.mojang.logging.LogUtils
import java.util.*
import kotlinx.coroutines.CancellationException

/**
 * Counts the online time of an `ONLINE` jail sentence. The per-player second counters are
 * server-thread confined; the remaining seconds are flushed to storage every five seconds and on
 * quit, and reaching zero hands the sentence to the expiry path.
 */
internal class JailOnlineTicker(
    private val runtime: JailRuntime,
    private val sentences: JailSentenceRepository,
    private val sentenceFor: (UUID) -> JailSentence?,
    private val onRemainingChanged: (JailSentence) -> Unit,
    private val onExpired: (UUID) -> Unit,
) {

    private val onlineTicks = HashMap<UUID, Int>()

    /**
     * Server-thread online-time tick for a single player. Decrements the remaining seconds of an
     * ONLINE sentence once per second; when it reaches zero the sentence is driven into release.
     * Remaining time is flushed to storage every five seconds and on quit.
     */
    fun onOnlineTick(playerId: UUID) {
        val sentence = sentenceFor(playerId) ?: return
        if (sentence.mode != JailSentenceMode.ONLINE || sentence.status != JailStatus.ACTIVE) {
            return
        }
        val remaining = sentence.remainingSeconds ?: return
        val ticks = (onlineTicks[playerId] ?: 0) + 1
        if (ticks < TICKS_PER_SECOND) {
            onlineTicks[playerId] = ticks
            return
        }
        onlineTicks[playerId] = 0
        val updated = JailSentenceMath.decrement(remaining, 1L)
        if (updated <= 0L) {
            onRemainingChanged(sentence.copy(remainingSeconds = 0L))
            onExpired(playerId)
            return
        }
        onRemainingChanged(sentence.copy(remainingSeconds = updated))
        if (updated % REMAINING_FLUSH_SECONDS == 0L) {
            persistRemaining(playerId, updated)
        }
    }

    /** Persists the remaining online seconds of a player's sentence and stops tracking them. */
    fun onPlayerQuit(playerId: UUID) {
        onlineTicks.remove(playerId)
        val sentence = sentenceFor(playerId) ?: return
        if (sentence.mode != JailSentenceMode.ONLINE) {
            return
        }
        val remaining = sentence.remainingSeconds ?: return
        persistRemaining(playerId, remaining)
    }

    /** Forgets every per-player tick counter, e.g. when the runtime stops. */
    fun clear() {
        onlineTicks.clear()
    }

    private fun persistRemaining(playerId: UUID, remainingSeconds: Long) {
        runtime.launch {
            try {
                sentences.updateRemaining(playerId, remainingSeconds)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                LOGGER.warn("failed to persist remaining jail time for {}", playerId, exception)
            }
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

        private const val TICKS_PER_SECOND: Int = 20
        private const val REMAINING_FLUSH_SECONDS: Long = 5L

    }

}
