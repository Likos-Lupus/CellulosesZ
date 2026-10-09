package top.likoslupus.cellulosesz.foundation.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Typed, immutable, transactional config store. Decodes a candidate, validates the complete
 * candidate, validates the transition from the current snapshot, and only then atomically
 * publishes it. A failed reload keeps the previous snapshot.
 *
 * The store starts [uninitialized][current]; [initialLoadBlocking] must run before any consumer
 * reads [current]. The initial load is deliberately blocking because configuration participates in
 * the boot decision (a missing or invalid config must stop startup, not be repaired lazily).
 *
 * This type is deliberately ignorant of any application schema and of the concrete text format;
 * callers supply a [ConfigDecoder], validation and (optionally) transition validation.
 */
public class ConfigStore<T>(
    private val path: Path,
    private val codec: ConfigDecoder<T>,
    private val validate: (T) -> List<ValidationError>,
    private val ioDispatcher: CoroutineDispatcher,
    private val validateTransition: (current: T, candidate: T) -> List<ValidationError> =
        { _, _ -> emptyList() },
) {

    private val snapshot = AtomicReference<T?>(null)
    private val generationCounter = AtomicInteger(0)

    public val initialized: Boolean
        get() = snapshot.get() != null

    public val current: T
        get() = snapshot.get()
            ?: error("configuration has not been loaded yet")

    public val generation: Int
        get() = generationCounter.get()

    /** Blocking, one-shot boot load. Call from the startup lifecycle, never on the server thread. */
    public fun initialLoadBlocking(): ConfigReloadResult =
        if (!Files.exists(path)) {
            ConfigReloadResult.Failure(
                listOf(
                    ValidationError(
                        "",
                        "config file not found: $path"
                    )
                )
            )
        } else {
            applyCandidate()
        }

    /** Transactional reload on the IO dispatcher. A failure keeps the previous snapshot. */
    public suspend fun reload(): ConfigReloadResult =
        withContext(ioDispatcher) {
            applyCandidate()
        }

    private fun applyCandidate(): ConfigReloadResult {
        val text = try {
            Files.readString(path)
        } catch (exception: IOException) {
            return failure("unable to read config file: ${exception.message}")
        }

        val candidate = try {
            codec.decode(text)
        } catch (exception: SerializationException) {
            return failure("invalid config: ${exception.message}")
        } catch (exception: IllegalArgumentException) {
            return failure("invalid config: ${exception.message}")
        }

        val structural = validate(candidate)
        if (structural.isNotEmpty()) {
            return ConfigReloadResult.Failure(structural)
        }

        val previous = snapshot.get()
        if (previous != null) {
            val transition = validateTransition(previous, candidate)
            if (transition.isNotEmpty()) {
                return ConfigReloadResult.Failure(transition)
            }
        }

        snapshot.set(candidate)
        return ConfigReloadResult.Success(generationCounter.incrementAndGet())
    }

    private fun failure(message: String): ConfigReloadResult.Failure =
        ConfigReloadResult.Failure(listOf(ValidationError("", message)))

}
