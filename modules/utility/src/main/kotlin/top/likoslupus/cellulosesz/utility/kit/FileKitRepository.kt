package top.likoslupus.cellulosesz.utility.kit

import com.mojang.logging.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.*

/**
 * The single JSON document holding every kit definition under `utility/kits.json`. A corrupt
 * document fails the whole catalog load (server-admin-owned state) and is never rewritten. A legacy
 * v1 `kits.json` is migrated once into v2 and renamed to `<file>.v1.bak`.
 */
internal class FileKitRepository(
    private val root: () -> Path,
    private val legacyRoot: () -> Path,
    private val codec: KitItemCodec,
) : KitRepository {

    private val mutex = Mutex()

    override suspend fun loadAll(): Map<KitName, KitDefinition> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                migrateIfNeeded()
                readFile().toDomain()
            }
        }

    override suspend fun create(definition: KitDefinition): Boolean =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readFile()
                if (current.kits.containsKey(definition.name.value)) {
                    return@withContext false
                }

                writeFile(
                    KitFile(
                        KIT_SCHEMA_VERSION,
                        current.kits + (definition.name.value to definition.toFile()),
                    )
                )

                true
            }
        }

    override suspend fun replace(definition: KitDefinition): Boolean =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readFile()
                if (!current.kits.containsKey(definition.name.value)) {
                    return@withContext false
                }

                writeFile(
                    KitFile(
                        KIT_SCHEMA_VERSION,
                        current.kits + (definition.name.value to definition.toFile()),
                    )
                )

                true
            }
        }

    override suspend fun remove(name: KitName): Boolean =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val current = readFile()
                if (!current.kits.containsKey(name.value)) {
                    return@withContext false
                }

                writeFile(
                    KitFile(
                        KIT_SCHEMA_VERSION,
                        current.kits - name.value,
                    )
                )

                true
            }
        }

    private fun file(): Path =
        root()
                .resolve("kits.json")

    private fun legacyFile(): Path =
        legacyRoot()
                .resolve("kits.json")

    private fun readFile(): KitFile {
        val target = file()
        if (!Files.exists(target)) {
            return KitFile()
        }

        val text = try {
            Files.readString(target)
        } catch (exception: IOException) {
            throw KitDataException("unable to read kits", exception)
        }

        val decoded = try {
            StorageJson.format.decodeFromString(
                KitFile.serializer(),
                text
            )
        } catch (exception: Exception) {
            throw KitDataException("corrupt kits data", exception)
        }

        validateDocument(target, decoded)
        return decoded
    }

    private fun validateDocument(file: Path, document: KitFile) {
        if (document.schemaVersion != KIT_SCHEMA_VERSION) {
            throw KitDataException("unsupported kits schema ${document.schemaVersion} in $file")
        }
        if (document.kits.size > MAX_STORED_KITS) {
            throw KitDataException("kits document exceeds the stored kit cap: $file")
        }

        document.kits.forEach { (rawName, definition) ->
            val name = KitName.parse(rawName)
                ?: throw KitDataException("kits document contains an invalid kit name '$rawName': $file")
            validateDefinition(name, definition, file)
        }
    }

    private fun validateDefinition(
        name: KitName,
        definition: KitDefinitionFile,
        file: Path
    ) {
        try {
            UUID.fromString(definition.id)
        } catch (_: IllegalArgumentException) {
            throw KitDataException("kit '${name.value}' has an invalid id: $file")
        }
        if (definition.items.size > MAX_STORED_ITEMS_PER_KIT) {
            throw KitDataException("kit '${name.value}' exceeds the stored item cap: $file")
        }

        when (definition.reuse) {
            KitReuseMode.ALWAYS,
            KitReuseMode.ONCE ->
                if (definition.cooldownSeconds != null) {
                    throw KitDataException(
                        "kit '${name.value}' has a cooldown but is not a cooldown kit: $file"
                    )
                }

            KitReuseMode.COOLDOWN ->
                if (definition.cooldownSeconds == null
                    || definition.cooldownSeconds <= 0L
                ) {
                    throw KitDataException(
                        "kit '${name.value}' is a cooldown kit without a positive cooldown: $file"
                    )
                }
        }
    }

    private fun writeFile(document: KitFile) {
        AtomicFile.writeUtf8AtomicallyBlocking(
            file(),
            StorageJson.format.encodeToString(
                KitFile.serializer(),
                document
            ),
        )
    }

    private fun KitFile.toDomain(): Map<KitName, KitDefinition> =
        kits.mapNotNull { (rawName, definition) ->
            val name = KitName.parse(rawName)
                ?: return@mapNotNull null

            name to definition.toDomain(name)
        }.toMap()

    private fun KitDefinitionFile.toDomain(name: KitName): KitDefinition {
        val items = this.items.mapIndexed { index, payload ->
            if (payload.length > MAX_ITEM_PAYLOAD_CHARS) {
                throw KitDataException("invalid item $index in kit '${name.value}': payload too large")
            }

            val stack = try {
                codec.decode(payload)
            } catch (exception: Exception) {
                throw KitDataException("invalid item $index in kit '${name.value}'", exception)
            }

            if (stack.isEmpty || stack.count <= 0) {
                throw KitDataException("invalid item $index in kit '${name.value}': empty stack")
            }

            stack
        }

        return KitDefinition(
            id = KitId(UUID.fromString(this.id)),
            name = name,
            reuse = reusePolicy(name),
            items = items,
        )
    }

    private fun KitDefinitionFile.reusePolicy(name: KitName): KitReusePolicy =
        when (reuse) {
            KitReuseMode.ALWAYS -> KitReusePolicy.Always
            KitReuseMode.ONCE -> KitReusePolicy.Once
            KitReuseMode.COOLDOWN -> KitReusePolicy.Cooldown(
                Duration.ofSeconds(
                    cooldownSeconds
                        ?: throw KitDataException("kit '${name.value}' is missing its cooldown")
                )
            )
        }

    private fun KitDefinition.toFile(): KitDefinitionFile {
        val mode: KitReuseMode
        val cooldown: Long?
        when (val policy = reuse) {
            KitReusePolicy.Always -> {
                mode = KitReuseMode.ALWAYS
                cooldown = null
            }

            KitReusePolicy.Once -> {
                mode = KitReuseMode.ONCE
                cooldown = null
            }

            is KitReusePolicy.Cooldown -> {
                mode = KitReuseMode.COOLDOWN
                cooldown = policy.duration.seconds
            }
        }

        return KitDefinitionFile(
            id = id.value.toString(),
            reuse = mode,
            cooldownSeconds = cooldown,
            items = items.map(codec::encode),
        )
    }

    /** One-way v1 -> v2 migration; runs only when v2 is absent and a legacy file is present. */
    private fun migrateIfNeeded() {
        val target = file()
        if (Files.exists(target)) {
            return
        }

        val legacy = legacyFile()
        if (!Files.exists(legacy)) {
            return
        }

        val text = try {
            Files.readString(legacy)
        } catch (exception: IOException) {
            throw KitDataException("unable to read legacy kits", exception)
        }

        val decoded = try {
            StorageJson.format.decodeFromString(LegacyKitFile.serializer(), text)
        } catch (exception: Exception) {
            throw KitDataException("corrupt legacy kits data", exception)
        }

        if (decoded.schemaVersion != 1) {
            throw KitDataException("unsupported legacy kits schema ${decoded.schemaVersion}")
        }
        if (decoded.kits.size > MAX_STORED_KITS) {
            throw KitDataException("legacy kits document exceeds the stored kit cap")
        }

        val migrated = decoded.kits.entries.associate { (rawName, payloads) ->
            val name = KitName.parse(rawName)
                ?: throw KitDataException("legacy kits document contains an invalid kit name '$rawName'")
            if (payloads.size > MAX_STORED_ITEMS_PER_KIT) {
                throw KitDataException("legacy kit '${name.value}' exceeds the stored item cap")
            }

            val items = payloads.mapIndexed { index, payload ->
                val stack = try {
                    codec.decode(payload)
                } catch (exception: Exception) {
                    throw KitDataException(
                        "invalid item $index in legacy kit '${name.value}'",
                        exception,
                    )
                }

                if (stack.isEmpty || stack.count <= 0) {
                    throw KitDataException("invalid item $index in legacy kit '${name.value}'")
                }

                stack
            }

            name.value to KitDefinition(
                id = KitId(UUID.randomUUID()),
                name = name,
                reuse = KitReusePolicy.Always,
                items = items,
            ).toFile()
        }

        writeFile(KitFile(KIT_SCHEMA_VERSION, migrated))

        // Read-back verification before retiring the legacy file.
        val verified = readFile()
        if (verified.kits.size != migrated.size) {
            throw KitDataException("kit migration read-back verification failed")
        }

        try {
            Files.move(
                legacy,
                legacy.resolveSibling("kits.json.v1.bak"),
                StandardCopyOption.REPLACE_EXISTING,
            )
            LOGGER.info("migrated legacy kits.json to v2; legacy retained as kits.json.v1.bak")
        } catch (exception: IOException) {
            LOGGER.warn(
                "kit migration succeeded but the legacy backup could not be renamed",
                exception
            )
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
