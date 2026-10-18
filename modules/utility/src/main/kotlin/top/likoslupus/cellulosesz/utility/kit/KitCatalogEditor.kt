package top.likoslupus.cellulosesz.utility.kit

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.utility.config.KitSettings
import java.time.Clock
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CancellationException

/**
 * The in-memory kit catalog. Entries, lifecycle state and change listeners are owned here and
 * mutated only on the server thread through [ServerThreadRunner].
 */
internal class KitCatalog(
    private val runner: ServerThreadRunner,
    private val clock: Clock,
) {

    private val entries = HashMap<KitName, KitDefinition>()

    private val listeners = CopyOnWriteArrayList<KitCatalogListener>()

    @Volatile private var currentState: KitCatalogState = KitCatalogState.Loading

    fun beginLoad() {
        currentState = KitCatalogState.Loading
    }

    fun fail(reason: String) {
        currentState = KitCatalogState.Failed(reason)
    }

    fun shutdown() {
        entries.clear()
        currentState = KitCatalogState.Loading
    }

    suspend fun install(loaded: Map<KitName, KitDefinition>) {
        runner.run {
            entries.clear()
            entries.putAll(loaded)
            currentState = KitCatalogState.Ready
        }
    }

    /** Server-thread-confined catalog resolution, distinguishing missing from not-yet-loaded. */
    suspend fun lookup(name: KitName): KitLookup =
        runner.run {
            when (currentState) {
                KitCatalogState.Loading -> KitLookup.Loading
                is KitCatalogState.Failed -> KitLookup.StorageUnavailable
                KitCatalogState.Ready -> entries[name]?.let(KitLookup::Found)
                    ?: KitLookup.NotFound
            }
        }

    /** Server-thread confined suggestion snapshot for Brigadier. */
    fun configuredNames(): List<String> =
        entries.keys.map { it.value }.sorted()

    /** Registers a catalog-mutation observer; close the handle to unregister it. */
    fun observe(listener: KitCatalogListener): KitCatalogRegistration {
        listeners += listener
        return KitCatalogRegistration { listeners.remove(listener) }
    }

    /** Server-thread-confined lifecycle state. */
    suspend fun lifecycleState(): KitCatalogState = runner.run { currentState }

    /** Server-thread-confined catalog size. */
    suspend fun size(): Int = runner.run { entries.size }

    /** Server-thread-confined lifecycle state paired with one entry. */
    suspend fun snapshot(name: KitName): Pair<KitCatalogState, KitDefinition?> =
        runner.run { currentState to entries[name] }

    /** Server-thread-confined full catalog snapshot. */
    suspend fun snapshotAll(): Pair<KitCatalogState, Map<KitName, KitDefinition>> =
        runner.run { currentState to HashMap(entries) }

    /** Publishes one inserted or replaced definition on the server thread. */
    suspend fun store(definition: KitDefinition, change: KitCatalogChange) {
        runner.run {
            entries[definition.name] = definition
            publish(definition.name, change)
        }
    }

    /** Publishes one removal on the server thread. */
    suspend fun delete(name: KitName) {
        runner.run {
            entries.remove(name)
            publish(name, KitCatalogChange.DELETED)
        }
    }

    private fun publish(name: KitName, change: KitCatalogChange) {
        val at = clock.instant()
        listeners.forEach { listener ->
            runCatching { listener.onChanged(name, change, at) }
                    .onFailure { LOGGER.warn("kit catalog listener failed", it) }
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}

/**
 * Catalog mutations (`create`/`update`/`delete`): validation, inventory capture, the durable
 * write and the in-memory catalog update.
 */
internal class KitCatalogEditor(
    private val definitions: KitRepository,
    private val capture: suspend (UUID, KitSettings) -> CaptureOutcome?,
    private val settings: () -> KitSettings,
    private val catalog: KitCatalog,
) {

    suspend fun create(
        playerId: UUID,
        rawName: String,
        reuse: KitReusePolicy,
    ): CreateKitResult {
        val config = settings()
        if (!config.enabled)
            return CreateKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return CreateKitResult.InvalidName
        if (reuse.cooldownTooLong(config)) {
            return CreateKitResult.CooldownTooLong
        }

        when (catalog.lifecycleState()) {
            KitCatalogState.Loading -> return CreateKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return CreateKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        if (catalog.size() >= config.maxKits) {
            return CreateKitResult.KitLimitReached
        }

        val items = capture(playerId, config)
            ?: return CreateKitResult.PlayerOffline

        when (items) {
            CaptureOutcome.Empty -> return CreateKitResult.EmptyInventory
            CaptureOutcome.TooMany -> return CreateKitResult.TooManyItems
            is CaptureOutcome.Items -> Unit
        }

        val captured = items.items
        val definition = KitDefinition(
            id = KitId(UUID.randomUUID()),
            name = name,
            reuse = reuse,
            items = captured,
        )

        return try {
            if (!definitions.create(definition)) {
                CreateKitResult.AlreadyExists
            } else {
                catalog.store(definition, KitCatalogChange.CREATED)
                CreateKitResult.Created(definition)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to create kit {}", name.value, exception)
            CreateKitResult.StorageUnavailable
        }
    }

    suspend fun update(
        playerId: UUID,
        rawName: String,
        reuse: KitReusePolicy?,
    ): UpdateKitResult {
        val config = settings()
        if (!config.enabled)
            return UpdateKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return UpdateKitResult.InvalidName

        val current = catalog.snapshot(name)
        when (current.first) {
            KitCatalogState.Loading -> return UpdateKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return UpdateKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        val existing = current.second
            ?: return UpdateKitResult.NotFound
        val policy = reuse ?: existing.reuse
        if (policy.cooldownTooLong(config))
            return UpdateKitResult.CooldownTooLong
        val items = capture(playerId, config)
            ?: return UpdateKitResult.PlayerOffline

        when (items) {
            CaptureOutcome.Empty -> return UpdateKitResult.EmptyInventory
            CaptureOutcome.TooMany -> return UpdateKitResult.TooManyItems
            is CaptureOutcome.Items -> Unit
        }

        val captured = items.items
        val definition = existing.copy(
            reuse = policy,
            items = captured,
        )

        return try {
            if (!definitions.replace(definition)) {
                UpdateKitResult.NotFound
            } else {
                catalog.store(definition, KitCatalogChange.UPDATED)
                UpdateKitResult.Updated(definition)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to update kit {}", name.value, exception)
            UpdateKitResult.StorageUnavailable
        }
    }

    suspend fun delete(rawName: String): DeleteKitResult {
        val config = settings()
        if (!config.enabled)
            return DeleteKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return DeleteKitResult.InvalidName

        when (catalog.lifecycleState()) {
            KitCatalogState.Loading -> return DeleteKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return DeleteKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        if (catalog.snapshot(name).second == null) {
            return DeleteKitResult.NotFound
        }

        return try {
            if (!definitions.remove(name)) {
                DeleteKitResult.NotFound
            } else {
                catalog.delete(name)
                DeleteKitResult.Deleted(name)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to delete kit {}", name.value, exception)
            DeleteKitResult.StorageUnavailable
        }
    }

    private fun KitReusePolicy.cooldownTooLong(config: KitSettings): Boolean {
        val limit = config.maxCooldownSeconds
            ?: return false
        val seconds = (this as? KitReusePolicy.Cooldown)?.duration?.seconds
            ?: return false
        return seconds > limit
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
