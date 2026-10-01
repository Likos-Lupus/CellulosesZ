package top.likoslupus.cellulosesz.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import top.likoslupus.cellulosesz.persistence.AtomicFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal sealed interface ConfigReloadResult {

    data class Success(val generation: Int) : ConfigReloadResult

    data class Failure(val errors: List<String>) : ConfigReloadResult

}

internal class ConfigService(private val path: Path) {

    private val snapshot = AtomicReference(CellulosesConfig())
    private val generationCounter = AtomicInteger(0)

    val current: CellulosesConfig get() = snapshot.get()

    val generation: Int get() = generationCounter.get()

    suspend fun loadOrCreate(): ConfigReloadResult =
        withContext(Dispatchers.IO) {
            if (!Files.exists(path)) {
                val default = CellulosesConfig()
                AtomicFile.writeUtf8Atomically(path, ConfigCodec.encode(default))
                snapshot.set(default)
                ConfigReloadResult.Success(generationCounter.incrementAndGet())
            } else {
                reloadInternal()
            }
        }

    suspend fun reload(): ConfigReloadResult =
        withContext(Dispatchers.IO) {
            reloadInternal()
        }

    private fun reloadInternal(): ConfigReloadResult {
        val text = try {
            Files.readString(path)
        } catch (exception: IOException) {
            return ConfigReloadResult.Failure(listOf("unable to read config file: ${exception.message}"))
        }

        val candidate = try {
            ConfigCodec.decode(text)
        } catch (exception: SerializationException) {
            return ConfigReloadResult.Failure(listOf("invalid config: ${exception.message}"))
        }

        return when (val validation = ConfigValidation.validate(candidate)) {
            is ConfigValidationResult.Invalid ->
                ConfigReloadResult.Failure(
                    validation.errors.map { "${it.path}: ${it.message}" }
                )

            ConfigValidationResult.Valid -> {
                snapshot.set(candidate)
                ConfigReloadResult.Success(generationCounter.incrementAndGet())
            }
        }
    }

}
