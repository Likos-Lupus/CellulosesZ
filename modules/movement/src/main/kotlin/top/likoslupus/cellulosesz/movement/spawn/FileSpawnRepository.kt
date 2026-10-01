package top.likoslupus.cellulosesz.movement.spawn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal class FileSpawnRepository(private val root: () -> Path) : SpawnRepository {

    private val mutex = Mutex()

    override suspend fun read(): StoredPosition? = mutex.withLock {
        withContext(Dispatchers.IO) {
            readFile().spawn
        }
    }

    override suspend fun write(position: StoredPosition?) = mutex.withLock {
        withContext(Dispatchers.IO) {
            writeFile(SpawnFile(SPAWN_SCHEMA_VERSION, position))
        }
    }

    private fun path(): Path = root().resolve("spawn.json")

    private fun readFile(): SpawnFile {
        val file = path()
        if (!Files.exists(file)) {
            return SpawnFile()
        }

        val text = try {
            Files.readString(file)
        } catch (exception: IOException) {
            throw SpawnDataException("unable to read spawn", exception)
        }

        val decoded = try {
            StorageJson.format.decodeFromString(
                SpawnFile.serializer(),
                text
            )
        } catch (exception: SerializationException) {
            throw SpawnDataException("corrupt spawn data", exception)
        }

        if (decoded.schemaVersion != SPAWN_SCHEMA_VERSION) {
            throw SpawnDataException("unsupported spawn schema ${decoded.schemaVersion}")
        }

        return decoded
    }

    private fun writeFile(file: SpawnFile) {
        AtomicFile.writeUtf8AtomicallyBlocking(
            path(),
            StorageJson.format.encodeToString(
                SpawnFile.serializer(),
                file
            ),
        )
    }

}
