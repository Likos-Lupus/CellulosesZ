package top.likoslupus.cellulosesz.warp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import top.likoslupus.cellulosesz.persistence.AtomicFile
import top.likoslupus.cellulosesz.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal class FileWarpRepository(private val root: () -> Path) : WarpRepository {

    private val mutex = Mutex()

    override suspend fun list(): List<Warp> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                readFile().warps.mapNotNull { (rawName, position) ->
                    WarpName.parse(rawName)?.let { Warp(it, position) }
                }.sortedBy { it.name.value }
            }
        }

    override suspend fun get(name: WarpName): Warp? =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                readFile().warps[name.value]?.let { Warp(name, it) }
            }
        }

    override suspend fun put(warp: Warp) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readFile()
                val updated = current.warps.toMutableMap()
                updated[warp.name.value] = warp.position
                writeFile(WarpFile(current.schemaVersion, updated))
            }
        }

    override suspend fun remove(name: WarpName): Boolean =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readFile()
                if (!current.warps.containsKey(name.value)) {
                    return@withContext false
                }
                val updated = current.warps.toMutableMap()
                updated.remove(name.value)
                writeFile(WarpFile(current.schemaVersion, updated))
                true
            }
        }

    private fun path(): Path =
        root().resolve("warps.json")

    private fun readFile(): WarpFile {
        val file = path()
        if (!Files.exists(file)) {
            return WarpFile()
        }

        val text = try {
            Files.readString(file)
        } catch (exception: IOException) {
            throw WarpDataException("unable to read warps", exception)
        }

        val decoded = try {
            StorageJson.format.decodeFromString(WarpFile.serializer(), text)
        } catch (exception: SerializationException) {
            throw WarpDataException("corrupt warps data", exception)
        }

        if (decoded.schemaVersion != WARP_SCHEMA_VERSION) {
            throw WarpDataException("unsupported warps schema ${decoded.schemaVersion}")
        }

        return decoded
    }

    private fun writeFile(file: WarpFile) {
        AtomicFile.writeUtf8AtomicallyBlocking(
            path(),
            StorageJson.format.encodeToString(
                WarpFile.serializer(),
                file
            ),
        )
    }

}
