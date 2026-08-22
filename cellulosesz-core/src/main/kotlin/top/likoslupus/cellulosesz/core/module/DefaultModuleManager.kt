package top.likoslupus.cellulosesz.core.module

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry
import top.likoslupus.cellulosesz.api.config.ConfigRegistry
import top.likoslupus.cellulosesz.api.config.ConfigSnapshot
import top.likoslupus.cellulosesz.api.event.EventRegistry
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger
import top.likoslupus.cellulosesz.api.module.*
import top.likoslupus.cellulosesz.api.scheduler.Scheduler
import top.likoslupus.cellulosesz.api.service.Registration
import top.likoslupus.cellulosesz.api.service.ServiceRegistry
import top.likoslupus.cellulosesz.core.config.ModulesConfig
import top.likoslupus.cellulosesz.core.legacy.LegacyFutureLifecycleAdapter
import top.likoslupus.cellulosesz.core.runtime.CellulosesRuntime
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class DefaultModuleManager(
    val catalog: ModuleCatalog,
    private val dataDirectory: Path,
    private val services: ServiceRegistry,
    private val configs: ConfigRegistry,
    private val events: EventRegistry,
    private val scheduler: Scheduler,
    private val middlewares: CommandMiddlewareRegistry,
    private val logger: CellulosesZLogger,
    val runtime: CellulosesRuntime = CellulosesRuntime(logger),
) {

    enum class ManagerState {

        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,

    }

    private class ModuleRuntimeRecord(
        val definition: ModuleDefinition,
        var state: ModuleLifecycleState = ModuleLifecycleState.DISCOVERED,
        var module: CellulosesZModule? = null,
        var context: DefaultModuleContext? = null,
        var optionalAvailability: Map<ModuleKey, Boolean> = emptyMap(),
    ) {

        val descriptor: ModuleDescriptor get() = definition.descriptor
        val key: ModuleKey get() = descriptor.key

    }

    private data class ReconcilePlan(
        val unloadOrder: List<ModuleKey>,
        val loadOrder: List<ModuleKey>,
        val unchangedOrder: List<ModuleKey>,
    )

    private val graph = ModuleGraph(catalog)
    private val records = ConcurrentHashMap<ModuleKey, ModuleRuntimeRecord>()
    private val activeKeysSnapshot = AtomicReference<Set<ModuleKey>>(emptySet())
    private val mutex = Mutex()

    private var managerState = ManagerState.NEW
    private var modulesConfig: ModulesConfig? = null
    private var modulesConfigRegistration: Registration? = null
    private var serverStarting = false
    private var serverStarted = false
    private var serverStopping = false

    init {
        catalog.definitions.forEach {
            records[it.descriptor.key] = ModuleRuntimeRecord(it)
        }
    }

    val state: ManagerState
        get() = managerState

    fun isModuleActive(key: ModuleKey): Boolean =
        key in activeKeysSnapshot.get()

    fun moduleEnabled(moduleId: String): Boolean =
        isModuleActive(ModuleKey(moduleId))

    fun activeCount(): Int =
        activeKeysSnapshot.get().size

    fun activeKeys(): Set<ModuleKey> =
        activeKeysSnapshot.get()

    fun modules(): List<LoadedModuleInfo> =
        catalog.descriptors.map {
            val record = records[it.key]
            val isActive = isModuleActive(it.key)
            LoadedModuleInfo(
                key = it.key,
                name = it.name,
                description = it.description,
                phase = it.phase,
                enabled = isActive,
                state = record?.state?.name ?: ModuleLifecycleState.DISCOVERED.name,
            )
        }

    suspend fun start() {
        mutex.withLock {
            if (managerState == ManagerState.RUNNING) {
                return
            }

            check(
                !(managerState == ManagerState.STOPPING
                        || managerState == ManagerState.STOPPED)
            ) {
                "Cannot start DefaultModuleManager; current state is $managerState"
            }

            managerState = ManagerState.STARTING

            val startedThisTransaction = mutableListOf<ModuleKey>()
            try {
                val reg = configs.register(
                    "modules",
                    ModulesConfig::class.java,
                    "modules.yml",
                    { defaultModulesConfig(catalog) },
                    "core"
                )
                modulesConfigRegistration = reg
                val loadedConfig = copyModulesConfig(
                    configs.require(
                        "modules",
                        ModulesConfig::class.java
                    )
                )
                modulesConfig = loadedConfig

                val desired = desiredEnabledKeys(loadedConfig)
                val plan = planReconciliation(desired)

                plan.loadOrder.forEach {
                    startModule(it)
                    startedThisTransaction.add(it)
                }
                managerState = ManagerState.RUNNING
                logger.info("Loaded ${activeCount()} CellulosesZ module(s).")
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    val unwindFailures = mutableListOf<Throwable>()
                    startedThisTransaction.asReversed().forEach {
                        try {
                            stopModule(it, serverShutdown = false)
                        } catch (t: Throwable) {
                            unwindFailures.add(t)
                        }
                    }
                    try {
                        modulesConfigRegistration?.close()
                    } catch (t: Throwable) {
                        unwindFailures.add(t)
                    }
                    modulesConfigRegistration = null
                    managerState = ManagerState.STOPPED
                    if (failure !is CancellationException) {
                        val aggregate = failure as? ModuleLoadException
                            ?: ModuleLoadException(
                                "Failed during initial module startup: ${failure.message}",
                                failure
                            )
                        unwindFailures.forEach { aggregate.addSuppressed(it) }
                        throw aggregate
                    }
                }
                throw failure
            }
        }
    }

    suspend fun stop() {
        mutex.withLock {
            if (managerState == ManagerState.STOPPED) {
                return
            }

            managerState = ManagerState.STOPPING

            val active = activeKeysSnapshot.get()
            val stopOrder = if (active.isNotEmpty()) {
                graph.resolve(active).stopKeys
            } else {
                emptyList()
            }

            val failures = mutableListOf<Throwable>()
            stopOrder.forEach {
                try {
                    stopModule(it, serverShutdown = true)
                } catch (t: Throwable) {
                    failures.add(t)
                }
            }

            modulesConfigRegistration?.close()
            modulesConfigRegistration = null

            managerState = ManagerState.STOPPED

            if (failures.isNotEmpty()) {
                val aggregate = IllegalStateException("DefaultModuleManager shutdown completed with ${failures.size} failure(s)")
                failures.forEach { aggregate.addSuppressed(it) }
                throw aggregate
            }
        }
    }

    suspend fun reconcile(desired: Set<ModuleKey>) {
        mutex.withLock {
            check(
                !(managerState == ManagerState.STOPPING
                        || managerState == ManagerState.STOPPED)
            ) {
                "Cannot reconcile modules; manager is in state $managerState"
            }

            val plan = planReconciliation(desired)
            val failures = mutableListOf<Throwable>()

            plan.unloadOrder.forEach {
                try {
                    stopModule(it, serverShutdown = false)
                } catch (t: Throwable) {
                    failures.add(t)
                }
            }

            val startedThisReconciliation = mutableListOf<ModuleKey>()
            try {
                plan.loadOrder.forEach {
                    startModule(it)
                    startedThisReconciliation.add(it)
                }
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    startedThisReconciliation.asReversed().forEach {
                        try {
                            stopModule(it, serverShutdown = false)
                        } catch (t: Throwable) {
                            failures.add(t)
                        }
                    }
                    if (failure !is CancellationException) {
                        failures.add(failure)
                    }
                }
                val aggregate = IllegalStateException("Failed during module reconciliation")
                failures.forEach { aggregate.addSuppressed(it) }
                if (failure is CancellationException) {
                    throw failure
                }
                throw aggregate
            }

            if (failures.isNotEmpty()) {
                val aggregate = IllegalStateException("Failed during module reconciliation")
                failures.forEach { aggregate.addSuppressed(it) }
                throw aggregate
            }
        }
    }

    suspend fun onServerStarting() {
        mutex.withLock {
            serverStarting = true
            val active = activeKeysSnapshot.get()
            if (active.isEmpty()) return
            val order = graph.resolve(active).startKeys
            for (key in order) {
                val record = records.getValue(key)
                val module = record.module ?: continue
                val context = record.context ?: continue
                runPhase(
                    key,
                    "server-starting"
                ) {
                    module.onServerStarting(context)
                }
            }
        }
    }

    suspend fun onServerStarted() {
        mutex.withLock {
            serverStarted = true
            val active = activeKeysSnapshot.get()
            if (active.isEmpty()) return
            val order = graph.resolve(active).startKeys
            for (key in order) {
                val record = records.getValue(key)
                val module = record.module ?: continue
                val context = record.context ?: continue
                runPhase(
                    key,
                    "server-started"
                ) {
                    module.onServerStarted(context)
                }
            }
        }
    }

    suspend fun onServerStopping() {
        mutex.withLock {
            serverStopping = true
            val active = activeKeysSnapshot.get()
            if (active.isEmpty()) return
            val stopOrder = graph.resolve(active).stopKeys
            val failures = mutableListOf<Throwable>()
            stopOrder.forEach {
                try {
                    stopModule(it, serverShutdown = true)
                } catch (t: Throwable) {
                    failures.add(t)
                }
            }
            if (failures.isNotEmpty()) {
                val aggregate = IllegalStateException("Module server stopping failed")
                failures.forEach { aggregate.addSuppressed(it) }
                throw aggregate
            }
        }
    }

    suspend fun prepareReload(
        candidateConfig: ModulesConfig,
        candidateSnapshot: ConfigSnapshot? = null,
    ): PreparedModuleReload {
        mutex.withLock {
            check(
                !(managerState == ManagerState.STOPPING
                        || managerState == ManagerState.STOPPED)
            ) {
                "Cannot reload modules; manager is in state $managerState"
            }

            val candidateDesired = desiredEnabledKeys(candidateConfig)
            val plan = planReconciliation(candidateDesired)

            val snapshotToUse = candidateSnapshot ?: object : ConfigSnapshot {
                override fun <T : Any> require(key: String, type: Class<T>): T =
                    configs.require(key, type)

                override fun <T : Any> optional(key: String, type: Class<T>): Optional<T> =
                    configs.optional(key, type)
            }

            val preparedEntries = mutableListOf<Pair<ModuleKey, PreparedModuleReload>>()
            try {
                for (key in plan.unchangedOrder) {
                    val record = records.getValue(key)
                    val module = record.module ?: continue
                    val context = record.context ?: continue
                    val reloadContext = DefaultModuleReloadContext(
                        context,
                        snapshotToUse,
                        ::moduleEnabled
                    )
                    val tx = module.prepareReload(reloadContext).await()
                    preparedEntries.add(key to tx)
                }
            } catch (failure: Throwable) {
                for ((_, tx) in preparedEntries.asReversed()) {
                    try {
                        tx.rollback().await()
                    } catch (_: Throwable) {
                    }
                }
                throw failure
            }

            return object : PreparedModuleReload {
                override fun commit(): CompletableFuture<Void> =
                    LegacyFutureLifecycleAdapter.futureVoid(runtime.dispatchers.application) {
                        mutex.withLock {
                            val unloadFailures = mutableListOf<Throwable>()
                            plan.unloadOrder.forEach { key ->
                                try {
                                    stopModule(key, serverShutdown = false)
                                } catch (t: Throwable) {
                                    unloadFailures.add(t)
                                }
                            }

                            val commitAttempted = mutableListOf<Pair<ModuleKey, PreparedModuleReload>>()
                            try {
                                preparedEntries.forEach { entry ->
                                    commitAttempted.add(entry)
                                    entry.second.commit().await()
                                }
                            } catch (commitFailure: Throwable) {
                                commitAttempted.asReversed().forEach { (_, tx) ->
                                    try {
                                        tx.rollback().await()
                                    } catch (_: Throwable) {
                                    }
                                }
                                throw commitFailure
                            }

                            plan.loadOrder.forEach { startModule(it) }

                            modulesConfig = copyModulesConfig(candidateConfig)

                            if (unloadFailures.isNotEmpty()) {
                                val aggregate = IllegalStateException("Module reload commit had unload failure(s)")
                                unloadFailures.forEach { aggregate.addSuppressed(it) }
                                throw aggregate
                            }
                        }
                    }

                override fun rollback(): CompletableFuture<Void> =
                    LegacyFutureLifecycleAdapter.futureVoid(runtime.dispatchers.application) {
                        mutex.withLock {
                            val rollbackFailures = mutableListOf<Throwable>()
                            preparedEntries.asReversed().forEach { (_, tx) ->
                                try {
                                    tx.rollback().await()
                                } catch (t: Throwable) {
                                    rollbackFailures.add(t)
                                }
                            }
                            if (rollbackFailures.isNotEmpty()) {
                                val ex = IllegalStateException("Module reload rollback failed")
                                rollbackFailures.forEach { ex.addSuppressed(it) }
                                throw ex
                            }
                        }
                    }
            }
        }
    }

    fun prepareReload(candidateSnapshot: ConfigSnapshot): CompletableFuture<PreparedModuleReload> {
        val candidateConfig = copyModulesConfig(
            candidateSnapshot.require(
                "modules",
                ModulesConfig::class.java
            )
        )
        return CoroutineScope(runtime.dispatchers.application).future {
            prepareReload(candidateConfig, candidateSnapshot)
        }
    }

    // Java future-bridge methods for compatibility with CellulosesZBootstrap
    fun loadAsync(): CompletableFuture<Void> =
        LegacyFutureLifecycleAdapter.futureVoid(runtime.dispatchers.application) {
            start()
        }

    fun onServerStartingAsync(): CompletableFuture<Void> =
        LegacyFutureLifecycleAdapter.futureVoid(runtime.dispatchers.application) {
            onServerStarting()
        }

    fun onServerStartedAsync(): CompletableFuture<Void> =
        LegacyFutureLifecycleAdapter.futureVoid(runtime.dispatchers.application) {
            onServerStarted()
        }

    fun onServerStoppingAsync(): CompletableFuture<Void> =
        LegacyFutureLifecycleAdapter.futureVoid(runtime.dispatchers.application) {
            onServerStopping()
        }

    private suspend fun startModule(key: ModuleKey) {
        val record = records.getValue(key)
        record.state = ModuleLifecycleState.RESOLVED
        record.state = ModuleLifecycleState.STARTING

        val module: CellulosesZModule
        try {
            module = record.definition.factory.create()
        } catch (t: Throwable) {
            record.state = ModuleLifecycleState.FAILED
            throw ModuleLoadException("Module '$key' factory failed", t)
        }

        val context = DefaultModuleContext(
            key = key,
            dataDirectory = dataDirectory.resolve(key.value),
            services = services,
            configs = configs,
            events = events,
            scheduler = scheduler,
            middlewares = middlewares,
            logger = logger,
            enabledPredicate = ::isModuleActive,
            runtime = runtime,
        )

        record.module = module
        record.context = context

        try {
            runPhase(key, "construct") { module.construct(context) }
            runPhase(key, "register-configs") { module.registerConfigs(context) }
            runPhase(key, "register-services") { module.registerServices(context) }

            context.initializables().forEach { it.initialize().await() }

            runPhase(key, "register-events") { module.registerEvents(context) }
            runPhase(key, "register-commands") { module.registerCommands(context) }

            if (serverStarting || serverStarted) {
                runPhase(key, "server-starting") { module.onServerStarting(context) }
            }
            if (serverStarted) {
                runPhase(key, "server-started") { module.onServerStarted(context) }
            }

            record.optionalAvailability = record.descriptor.optional.associateWith {
                isModuleActive(
                    it
                )
            }

            record.state = ModuleLifecycleState.ACTIVE
            updateActiveSnapshot()
            logger.info("Loaded module: $key")
        } catch (failure: Throwable) {
            record.state = ModuleLifecycleState.FAILED
            val scopeFailures = mutableListOf<Throwable>()
            withContext(NonCancellable) {
                try {
                    context.scope.close()
                } catch (t: Throwable) {
                    scopeFailures.add(t)
                }
            }
            record.module = null
            record.context = null
            updateActiveSnapshot()

            (failure as? CancellationException)?.let { throw it }

            val loadEx = failure as? ModuleLoadException
                ?: ModuleLoadException(
                    "Module '$key' failed during startup",
                    failure
                )
            scopeFailures.forEach { loadEx.addSuppressed(it) }
            throw loadEx
        }
    }

    private suspend fun stopModule(key: ModuleKey, serverShutdown: Boolean) {
        val record = records.getValue(key)
        if (
            record.state != ModuleLifecycleState.ACTIVE &&
            record.state != ModuleLifecycleState.STARTING &&
            record.state != ModuleLifecycleState.STOPPING
        ) {
            return
        }

        record.state = ModuleLifecycleState.STOPPING
        updateActiveSnapshot()

        val module = record.module
        val context = record.context
        val failures = mutableListOf<Throwable>()

        if (module != null && context != null) {
            withContext(NonCancellable) {
                try {
                    module.onUnload(context)
                } catch (t: Throwable) {
                    failures.add(t)
                }

                if (serverShutdown || serverStopping) {
                    try {
                        module.onServerStopping(context)
                    } catch (t: Throwable) {
                        failures.add(t)
                    }
                }

                try {
                    context.scope.close()
                } catch (t: Throwable) {
                    failures.add(t)
                }
            }
        }

        record.module = null
        record.context = null
        record.optionalAvailability = emptyMap()
        record.state = ModuleLifecycleState.STOPPED
        updateActiveSnapshot()

        logger.info("Unloaded module: $key")

        if (failures.isNotEmpty()) {
            val aggregate = IllegalStateException("Failed to unload module $key")
            failures.forEach { aggregate.addSuppressed(it) }
            throw aggregate
        }
    }

    private fun updateActiveSnapshot() {
        val active = records.values
                .filter { it.state == ModuleLifecycleState.ACTIVE }
                .map { it.key }
                .toSet()
        activeKeysSnapshot.set(active)
    }

    private fun planReconciliation(desired: Set<ModuleKey>): ReconcilePlan {
        graph.resolve(desired)

        val currentActive = activeKeysSnapshot.get()

        val restart = currentActive
                .filter { it in desired }
                .filter { key ->
                    val record = records.getValue(key)
                    val currentAvail = record.optionalAvailability
                    val desiredAvail = record.descriptor.optional.associateWith { it in desired }
                    currentAvail != desiredAvail
                }
                .toMutableSet()

        expandRequiredDependents(restart, currentActive, desired)

        val toDisable = (currentActive - desired + restart).toMutableSet()
        val toEnable = (desired - currentActive + restart).toMutableSet()
        val unchanged = (currentActive.intersect(desired) - restart)

        val unloadOrder = if (currentActive.isNotEmpty()) {
            graph.resolve(currentActive).stopKeys.filter { it in toDisable }
        } else {
            emptyList()
        }

        val loadOrder = if (desired.isNotEmpty()) {
            graph.resolve(desired).startKeys.filter { it in toEnable }
        } else {
            emptyList()
        }

        val unchangedOrder = if (desired.isNotEmpty()) {
            graph.resolve(desired).startKeys.filter { it in unchanged }
        } else {
            emptyList()
        }

        return ReconcilePlan(
            unloadOrder = unloadOrder,
            loadOrder = loadOrder,
            unchangedOrder = unchangedOrder,
        )
    }

    private fun expandRequiredDependents(
        restart: MutableSet<ModuleKey>,
        current: Set<ModuleKey>,
        desired: Set<ModuleKey>,
    ) {
        var changed = true
        while (changed) {
            changed = false
            for (activeKey in current) {
                if (activeKey in restart || activeKey !in desired) {
                    continue
                }
                val record = records.getValue(activeKey)
                if (
                    record.descriptor.requires.any { it in restart }
                    && restart.add(activeKey)
                ) {
                    changed = true
                }
            }
        }
    }

    private fun desiredEnabledKeys(config: ModulesConfig): Set<ModuleKey> {
        config.modules.keys.forEach {
            val key = ModuleKey(it)
            if (!records.containsKey(key)) {
                logger.warn("Ignoring unknown module id in modules.yml: $it")
            }
        }

        return records.values
                .filter { record ->
                    config.modules.getOrDefault(
                        record.key.value,
                        record.descriptor.enabledByDefault
                    )
                }
                .map { it.key }
                .toSet()
    }

    private fun defaultModulesConfig(catalog: ModuleCatalog): ModulesConfig {
        val config = ModulesConfig()
        catalog.descriptors.forEach { (key, _, _, _, _, _, _, enabledByDefault) ->
            config.modules[key.value] = enabledByDefault
        }
        return config
    }

    private fun copyModulesConfig(source: ModulesConfig): ModulesConfig {
        val copy = ModulesConfig()
        copy.modules.putAll(source.modules)
        return copy
    }

    private inline fun runPhase(
        key: ModuleKey,
        phase: String,
        action: () -> Unit
    ) {
        try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            throw ModuleLoadException("Module '$key' failed during startup stage '$phase'", failure)
        }
    }

}
