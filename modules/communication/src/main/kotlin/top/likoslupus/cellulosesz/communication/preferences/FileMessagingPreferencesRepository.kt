package top.likoslupus.cellulosesz.communication.preferences

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Per-player preference files under `communication/preferences/<uuid>.json`. A corrupt file is
 * never overwritten; the caller marks the player unavailable instead of silently resetting them.
 */
internal class FileMessagingPreferencesRepository(
    private val dataRoot: () -> Path,
) : MessagingPreferencesRepository {

    private val locks = KeyedMutex<UUID>()

    override suspend fun load(playerId: UUID): MessagingPreferences? =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                val file = path(playerId)
                if (!Files.exists(file)) {
                    return@withContext null
                }

                val text = try {
                    Files.readString(file)
                } catch (exception: IOException) {
                    throw MessagingPreferencesDataException(
                        "unable to read preferences for $playerId",
                        exception,
                    )
                }

                val decoded = try {
                    StorageJson.format.decodeFromString(
                        MessagingPreferencesFile.serializer(),
                        text,
                    )
                } catch (exception: Exception) {
                    throw MessagingPreferencesDataException(
                        "corrupt preferences for $playerId",
                        exception,
                    )
                }

                validate(file, playerId, decoded)
                decoded.toDomain(playerId)
            }
        }

    override suspend fun save(
        playerId: UUID,
        playerName: String,
        preferences: MessagingPreferences,
    ) =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                AtomicFile.writeUtf8AtomicallyBlocking(
                    path(playerId),
                    StorageJson.format.encodeToString(
                        MessagingPreferencesFile.serializer(),
                        preferences.toFile(playerId, playerName),
                    ),
                )
            }
        }

    override suspend fun delete(playerId: UUID): Boolean =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(path(playerId))
            }
        }

    private fun validate(
        file: Path,
        expectedId: UUID,
        decoded: MessagingPreferencesFile,
    ) {
        if (decoded.schemaVersion != PREFERENCES_SCHEMA_VERSION) {
            throw MessagingPreferencesDataException(
                "unsupported preferences schema ${decoded.schemaVersion} in $file"
            )
        }
        if (decoded.playerId != expectedId.toString()) {
            throw MessagingPreferencesDataException(
                "preferences file name does not match its playerId: $file"
            )
        }
        if (decoded.playerName.isBlank()) {
            throw MessagingPreferencesDataException(
                "preferences file has a blank player name: $file"
            )
        }
        if (decoded.ignoredPlayerIds.size > MAX_STORED_IGNORES) {
            throw MessagingPreferencesDataException(
                "preferences file exceeds the ignore cap: $file"
            )
        }
        decoded.ignoredPlayerIds.forEach { raw ->
            try {
                UUID.fromString(raw)
            } catch (_: IllegalArgumentException) {
                throw MessagingPreferencesDataException(
                    "preferences file contains an invalid ignored UUID '$raw': $file"
                )
            }
        }
    }

    private fun directory(): Path =
        dataRoot()
                .resolve("preferences")

    private fun path(playerId: UUID): Path =
        directory()
                .resolve("$playerId.json")

}
