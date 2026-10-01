package top.likoslupus.cellulosesz.movement.teleport.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

internal class FileTeleportHistoryRepository(private val root: () -> Path) :
    TeleportHistoryRepository {

    private val locks = KeyedMutex<UUID>()

    override suspend fun read(playerId: UUID): StoredPosition? =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                readFile(playerId).previous
            }
        }

    override suspend fun write(playerId: UUID, position: StoredPosition) =
        locks.withLock(playerId) {
            withContext(Dispatchers.IO) {
                AtomicFile.writeUtf8AtomicallyBlocking(
                    path(playerId),
                    StorageJson.format.encodeToString(
                        TeleportHistoryFile.serializer(),
                        TeleportHistoryFile(TELEPORT_HISTORY_SCHEMA_VERSION, position),
                    ),
                )
            }
        }

    private fun path(playerId: UUID): Path =
        root().resolve("teleport-history").resolve("$playerId.json")

    private fun readFile(playerId: UUID): TeleportHistoryFile {
        val file = path(playerId)
        if (!Files.exists(file)) {
            return TeleportHistoryFile()
        }

        val text = try {
            Files.readString(file)
        } catch (exception: IOException) {
            throw TeleportHistoryDataException(
                "unable to read teleport history for $playerId",
                exception
            )
        }

        val decoded = try {
            StorageJson.format.decodeFromString(
                TeleportHistoryFile.serializer(),
                text
            )
        } catch (exception: IllegalArgumentException) {
            throw TeleportHistoryDataException("corrupt teleport history for $playerId", exception)
        }

        if (decoded.schemaVersion != TELEPORT_HISTORY_SCHEMA_VERSION) {
            throw TeleportHistoryDataException("unsupported teleport history schema ${decoded.schemaVersion}")
        }

        return decoded
    }

}
