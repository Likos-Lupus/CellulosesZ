package top.likoslupus.cellulosesz.utility.kit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal class FileKitRepository(private val root: () -> Path) : KitRepository {

    private val mutex = Mutex()

    override suspend fun names(): List<KitName> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                readFile().kits.keys.mapNotNull(KitName::parse).sortedBy { it.value }
            }
        }

    override suspend fun load(name: KitName): List<ItemStack>? =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                readFile().kits[name.value]?.map(KitItemCodec::decode)
            }
        }

    override suspend fun save(name: KitName, items: List<ItemStack>) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readFile()
                val updated = current.kits.toMutableMap()
                updated[name.value] = items.map(KitItemCodec::encode)
                writeFile(KitFile(current.schemaVersion, updated))
            }
        }

    override suspend fun remove(name: KitName): Boolean =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readFile()
                if (!current.kits.containsKey(name.value)) {
                    return@withContext false
                }

                val updated = current.kits.toMutableMap()
                updated.remove(name.value)
                writeFile(KitFile(current.schemaVersion, updated))
                true
            }
        }

    private fun path(): Path =
        root().resolve("kits.json")

    private fun readFile(): KitFile {
        val file = path()
        if (!Files.exists(file)) {
            return KitFile()
        }

        val text = try {
            Files.readString(file)
        } catch (exception: IOException) {
            throw KitDataException("unable to read kits", exception)
        }

        val decoded = try {
            StorageJson.format.decodeFromString(KitFile.serializer(), text)
        } catch (exception: SerializationException) {
            throw KitDataException("corrupt kits data", exception)
        }

        if (decoded.schemaVersion != KIT_SCHEMA_VERSION) {
            throw KitDataException("unsupported kits schema ${decoded.schemaVersion}")
        }

        return decoded
    }

    private fun writeFile(file: KitFile) {
        AtomicFile.writeUtf8AtomicallyBlocking(
            path(),
            StorageJson.format.encodeToString(
                KitFile.serializer(),
                file
            ),
        )
    }

}
