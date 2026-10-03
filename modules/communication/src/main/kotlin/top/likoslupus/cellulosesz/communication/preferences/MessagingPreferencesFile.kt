package top.likoslupus.cellulosesz.communication.preferences

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.communication.config.ReplyMode
import java.util.*

internal const val PREFERENCES_SCHEMA_VERSION: Int = 1

/** Machine safety cap, deliberately far above the configurable product limit. */
internal const val MAX_STORED_IGNORES: Int = 10_000

@Serializable
internal data class MessagingPreferencesFile(
    val schemaVersion: Int = PREFERENCES_SCHEMA_VERSION,
    val playerId: String,
    val playerName: String,
    val receivePrivateMessages: Boolean = true,
    val ignoredPlayerIds: Set<String> = emptySet(),
    val replyMode: ReplyMode? = null,
)

internal fun MessagingPreferencesFile.toDomain(ownerId: UUID): MessagingPreferences =
    MessagingPreferences(
        receivePrivateMessages = receivePrivateMessages,
        ignoredPlayerIds = ignoredPlayerIds
                .map(UUID::fromString)
                .filter { it != ownerId }
                .toSet(),
        replyMode = replyMode,
    )

internal fun MessagingPreferences.toFile(
    playerId: UUID,
    playerName: String,
): MessagingPreferencesFile =
    MessagingPreferencesFile(
        playerId = playerId.toString(),
        playerName = playerName,
        receivePrivateMessages = receivePrivateMessages,
        ignoredPlayerIds = ignoredPlayerIds.map(UUID::toString).toSet(),
        replyMode = replyMode,
    )
