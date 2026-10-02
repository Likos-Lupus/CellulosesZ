package top.likoslupus.cellulosesz.administration.moderation.mute

import com.mojang.logging.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Per-player mute files under `moderation/mutes/<uuid>.json`. A corrupt file is isolated and never
 * overwritten; the affected UUID is reported so the in-memory state can be marked unavailable.
 */
internal class FileMuteRepository(
    private val dataRoot: () -> Path,
) : MuteRepository {

    private val locks = KeyedMutex<UUID>()

    override suspend fun loadAll(): MuteLoadResult =
        withContext(Dispatchers.IO) {
            val directory = directory()
            if (!Files.isDirectory(directory)) {
                return@withContext MuteLoadResult(
                    emptyList(),
                    emptySet()
                )
            }

            val records = mutableListOf<Mute>()
            val corrupt = mutableSetOf<UUID>()

            Files.newDirectoryStream(directory).use {
                it.forEach { entry ->
                    val fileName = entry.fileName.toString()
                    if (!Files.isRegularFile(entry)
                        || !fileName.endsWith(".json")
                    ) {
                        return@forEach
                    }

                    val id = parseFileName(fileName)
                    if (id == null) {
                        LOGGER.error("ignoring mute file with non-UUID name {}", fileName)
                        return@forEach
                    }

                    try {
                        val text = Files.readString(entry)
                        val decoded = StorageJson.format.decodeFromString(
                            MuteFile.serializer(),
                            text,
                        )
                        validate(entry, id, decoded)
                        records += decoded.toDomain()
                    } catch (exception: Exception) {
                        LOGGER.error(
                            "corrupt mute record {} is kept on disk and marked unavailable",
                            fileName,
                            exception,
                        )
                        corrupt += id
                    }
                }
            }

            MuteLoadResult(records, corrupt)
        }

    override suspend fun put(mute: Mute) =
        locks.withLock(mute.playerId) {
            withContext(Dispatchers.IO) {
                AtomicFile.writeUtf8AtomicallyBlocking(
                    path(mute.playerId),
                    StorageJson.format.encodeToString(
                        MuteFile.serializer(),
                        mute.toFile()
                    ),
                )
            }
        }

    override suspend fun remove(playerId: UUID): Boolean =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(path(playerId))
            }
        }

    private fun validate(
        file: Path,
        expectedId: UUID,
        decoded: MuteFile
    ) {
        if (decoded.schemaVersion != MUTE_SCHEMA_VERSION) {
            throw MuteDataException("unsupported mute schema ${decoded.schemaVersion} in $file")
        }
        if (decoded.playerId != expectedId.toString()) {
            throw MuteDataException("mute file name does not match its playerId: $file")
        }
        if (decoded.playerName.isBlank()) {
            throw MuteDataException("mute file has a blank player name: $file")
        }
        val issued = decoded.issuedAtEpochMillis
        val expires = decoded.expiresAtEpochMillis
        if (expires != null && expires <= issued) {
            throw MuteDataException("mute file expiry is not after issue time: $file")
        }
    }

    private fun parseFileName(fileName: String): UUID? =
        try {
            UUID.fromString(fileName.removeSuffix(".json"))
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun directory(): Path =
        dataRoot().resolve("moderation").resolve("mutes")

    private fun path(playerId: UUID): Path =
        directory().resolve("$playerId.json")

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
