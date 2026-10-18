package top.likoslupus.cellulosesz.administration.jail

import java.time.Instant
import java.util.*

/** Jail sentence storage. One row per jailed player, keyed by namespace and player. */
internal interface JailSentenceRepository {

    suspend fun loadAll(): List<JailSentence>

    suspend fun put(sentence: JailSentence)

    suspend fun updateStatus(
        playerId: UUID,
        status: JailStatus,
        releaseRequestedAt: Instant?,
    ): Boolean

    suspend fun remove(playerId: UUID): Boolean

    /** Persists the remaining online seconds of an ONLINE sentence. */
    suspend fun updateRemaining(playerId: UUID, remainingSeconds: Long): Boolean

}
