package top.likoslupus.cellulosesz.foundation.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import top.likoslupus.cellulosesz.foundation.persistence.AtomicFile
import top.likoslupus.cellulosesz.foundation.persistence.StorageJson
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Typed, immutable, transactional config store. Decodes a candidate, validates the complete
 * candidate, and only then atomically publishes it. A failed reload keeps the previous snapshot.
 *
 * This type is deliberately ignorant of any application schema; feature owners supply their own
 * serializer, defaults, and validation.
 */
public class ConfigStore<T>(
    private val path: Path,
    private val serializer: KSerializer<T>,
    private val defaultValue: () -> T,
    private val validate: (T) -> List<ValidationError>,
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val snapshot = AtomicReference(defaultValue())
    private val generationCounter = AtomicInteger(0)

    public val current: T get() = snapshot.get()

    public val generation: Int get() = generationCounter.get()

    public suspend fun loadOrCreate(): ConfigReloadResult =
        withContext(ioDispatcher) {
            if (!Files.exists(path)) {
                val default = defaultValue()
                AtomicFile.writeUtf8AtomicallyBlocking(path, encode(default))
                snapshot.set(default)
                ConfigReloadResult.Success(generationCounter.incrementAndGet())
            } else {
                reloadInternal()
            }
        }

    public suspend fun reload(): ConfigReloadResult =
        withContext(ioDispatcher) {
            reloadInternal()
        }

    private fun reloadInternal(): ConfigReloadResult {
        val text = try {
            Files.readString(path)
        } catch (exception: IOException) {
            return ConfigReloadResult.Failure(
                listOf(
                    ValidationError(
                        "",
                        "unable to read config file: ${exception.message}"
                    )
                )
            )
        }

        val candidate = try {
            StorageJson.format.decodeFromString(serializer, text)
        } catch (exception: SerializationException) {
            return ConfigReloadResult.Failure(
                listOf(
                    ValidationError(
                        "",
                        "invalid config: ${exception.message}"
                    )
                )
            )
        }

        val errors = validate(candidate)
        return if (errors.isEmpty()) {
            snapshot.set(candidate)
            ConfigReloadResult.Success(generationCounter.incrementAndGet())
        } else {
            ConfigReloadResult.Failure(errors)
        }
    }

    private fun encode(value: T): String =
        StorageJson.format.encodeToString(serializer, value)

}
