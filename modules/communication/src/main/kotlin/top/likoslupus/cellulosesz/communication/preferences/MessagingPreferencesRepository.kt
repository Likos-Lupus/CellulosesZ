package top.likoslupus.cellulosesz.communication.preferences

import java.util.*

internal interface MessagingPreferencesRepository {

    /** Returns `null` when the player has no stored preferences yet. */
    suspend fun load(playerId: UUID): MessagingPreferences?

    suspend fun save(
        playerId: UUID,
        playerName: String,
        preferences: MessagingPreferences,
    )

    suspend fun delete(playerId: UUID): Boolean

}

internal class MessagingPreferencesDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(
    message,
    cause,
)
