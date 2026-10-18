package top.likoslupus.cellulosesz.foundation.config

import dev.eav.tomlkt.Toml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

/**
 * Multi-file configuration: every [ConfigFileSpec] is read as an independent unit and only its own
 * scopes are decoded. A table owned by a different file is ignored rather than merged, while an
 * unknown top-level table is reported so a typo cannot silently disable a whole section.
 *
 * A reload decodes and validates every file before publishing; any error keeps the previous
 * snapshot. Each error carries the owning file name via [ValidationError.source].
 */
public class BundleConfigStore<T>(
    private val directory: Path,
    private val files: List<ConfigFileSpec>,
    private val compose: (ConfigValues) -> T,
    private val validate: (T) -> List<ValidationError> = { emptyList() },
    private val validateTransition: (current: T, candidate: T) -> List<ValidationError> = { _, _ ->
        emptyList()
    },
    private val ioDispatcher: CoroutineDispatcher,
) : ConfigHandle<T> {

    private val snapshot = AtomicReference<T?>(null)
    private val generationCounter = AtomicInteger(0)
    private val toml = Toml { ignoreUnknownKeys = false }
    private val knownScopes: Set<String> = files.flatMap { it.allowedScopes }.toSet()
    private val fileByScope: Map<String, String> =
        files.flatMap { file -> file.allowedScopes.map { it to file.fileName } }.toMap()

    override val initialized: Boolean
        get() = snapshot.get() != null

    override val current: T
        get() = snapshot.get()
            ?: error("configuration has not been loaded yet")

    override val generation: Int
        get() = generationCounter.get()

    override fun initialLoadBlocking(): ConfigReloadResult = applyCandidate()

    override suspend fun reload(): ConfigReloadResult =
        withContext(ioDispatcher) {
            applyCandidate()
        }

    private fun applyCandidate(): ConfigReloadResult {
        val values = LinkedHashMap<String, Any>()
        val errors = mutableListOf<ValidationError>()

        files.forEach { file ->
            val path = directory.resolve(file.fileName)
            if (!Files.exists(path)) {
                // A deleted file is treated as a reset of its scopes; the template is rewritten on
                // the next start, never during a reload.
                file.scopes.forEach { spec -> values[spec.scope] = spec.defaults }
                return@forEach
            }
            val text = try {
                Files.readString(path)
            } catch (exception: IOException) {
                errors += ValidationError(
                    path = file.fileName,
                    message = "unable to read: ${exception.message}",
                    source = file.fileName,
                )
                return@forEach
            }
            val root = try {
                toml.parseToTomlTable(text)
            } catch (exception: SerializationException) {
                errors += ValidationError(
                    path = file.fileName,
                    message = "invalid TOML: ${exception.message}",
                    source = file.fileName,
                )
                return@forEach
            }

            root.keys.forEach { key ->
                when (key) {
                    in file.allowedScopes -> Unit
                    in knownScopes -> Unit // owned by another file: ignored here
                    else -> errors += ValidationError(
                        path = "${file.fileName}.$key",
                        message = "unknown top-level table '$key'",
                        source = file.fileName,
                    )
                }
            }

            file.scopes.forEach { spec ->
                val element = root[spec.scope]
                if (element == null) {
                    values[spec.scope] = spec.defaults
                    return@forEach
                }
                try {
                    @Suppress("UNCHECKED_CAST")
                    val decoded = toml.decodeFromTomlElement(
                        spec.serializer as KSerializer<Any>,
                        element,
                    )
                    values[spec.scope] = decoded
                } catch (exception: SerializationException) {
                    errors += ValidationError(
                        path = "${file.fileName}.${spec.scope}",
                        message = "invalid config: ${exception.message}",
                        source = file.fileName,
                    )
                } catch (exception: IllegalArgumentException) {
                    errors += ValidationError(
                        path = "${file.fileName}.${spec.scope}",
                        message = "invalid config: ${exception.message}",
                        source = file.fileName,
                    )
                }
            }
        }

        if (errors.isNotEmpty()) {
            return ConfigReloadResult.Failure(errors)
        }

        val candidate = compose(ConfigValues(values))
        val structural = validate(candidate).map { it.withSource(fileFor(it.path)) }
        if (structural.isNotEmpty()) {
            return ConfigReloadResult.Failure(structural)
        }

        val previous = snapshot.get()
        if (previous != null) {
            val transition = validateTransition(previous, candidate)
                    .map { it.withSource(fileFor(it.path)) }
            if (transition.isNotEmpty()) {
                return ConfigReloadResult.Failure(transition)
            }
        }

        snapshot.set(candidate)
        return ConfigReloadResult.Success(generationCounter.incrementAndGet())
    }

    private fun fileFor(path: String): String {
        val scope = path.substringBefore('.')
        return fileByScope[scope]
            ?: files.first().fileName
    }

    private fun ValidationError.withSource(source: String?): ValidationError =
        when {
            this.source != null || source == null -> this
            else -> copy(source = source)
        }

}
