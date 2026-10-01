package top.likoslupus.cellulosesz.movement.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

internal class FileHomeRepository(private val root: () -> Path) : HomeRepository {

    private val locks = KeyedMutex<UUID>()

    override suspend fun list(owner: UUID): List<Home> =
        locks.withLock(owner) {
            withContext(Dispatchers.IO) {
                readFile(owner).homes.mapNotNull { (rawName, position) ->
                    HomeName.parse(rawName)?.let { Home(it, position) }
                }.sortedBy { it.name.value }
            }
        }

    override suspend fun put(owner: UUID, home: Home) =
        locks.withLock(owner) {
            withContext(Dispatchers.IO) {
                val current = readFile(owner)
                val updated = current.homes.toMutableMap()
                updated[home.name.value] = home.position
                writeFile(owner, HomeFile(current.schemaVersion, updated))
            }
        }

    override suspend fun remove(owner: UUID, name: HomeName): Boolean =
        locks.withLock(owner) {
            withContext(Dispatchers.IO) {
                val current = readFile(owner)
                if (!current.homes.containsKey(name.value)) {
                    return@withContext false
                }
                val updated = current.homes.toMutableMap()
                updated.remove(name.value)
                writeFile(owner, HomeFile(current.schemaVersion, updated))
                true
            }
        }

    private fun path(owner: UUID): Path =
        root().resolve("homes").resolve("$owner.json")

    private fun readFile(owner: UUID): HomeFile {
        val file = path(owner)
        if (!Files.exists(file)) {
            return HomeFile()
        }

        val text = try {
            Files.readString(file)
        } catch (exception: IOException) {
            throw HomeDataException("unable to read homes for $owner", exception)
        }

        val decoded = try {
            StorageJson.format.decodeFromString(HomeFile.serializer(), text)
        } catch (exception: SerializationException) {
            throw HomeDataException("corrupt homes data for $owner", exception)
        }

        if (decoded.schemaVersion != HOME_SCHEMA_VERSION) {
            throw HomeDataException("unsupported homes schema ${decoded.schemaVersion} for $owner")
        }

        return decoded
    }

    private fun writeFile(owner: UUID, file: HomeFile) {
        AtomicFile.writeUtf8AtomicallyBlocking(
            path(owner),
            StorageJson.format.encodeToString(
                HomeFile.serializer(),
                file
            ),
        )
    }

}
