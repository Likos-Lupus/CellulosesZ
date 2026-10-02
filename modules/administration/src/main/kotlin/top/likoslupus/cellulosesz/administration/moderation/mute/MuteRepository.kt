package top.likoslupus.cellulosesz.administration.moderation.mute

import java.util.*

internal data class MuteLoadResult(
    val records: List<Mute>,
    val corruptPlayerIds: Set<UUID>,
)

/** Active mute storage. Only active mutes are stored; full history lives in the audit log. */
internal interface MuteRepository {

    suspend fun loadAll(): MuteLoadResult

    suspend fun put(mute: Mute)

    suspend fun remove(playerId: UUID): Boolean

}

internal class MuteDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(
    message,
    cause,
)
