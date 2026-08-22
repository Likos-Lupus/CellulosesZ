package top.likoslupus.cellulosesz.core.module

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger
import top.likoslupus.cellulosesz.api.module.*
import top.likoslupus.cellulosesz.core.command.execution.DefaultCommandExecutionPipeline
import top.likoslupus.cellulosesz.core.config.JacksonConfigRegistry
import top.likoslupus.cellulosesz.core.config.ModulesConfig
import top.likoslupus.cellulosesz.core.event.SimpleEventRegistry
import top.likoslupus.cellulosesz.core.runtime.CellulosesRuntime
import top.likoslupus.cellulosesz.core.scheduler.DefaultScheduler
import top.likoslupus.cellulosesz.core.service.DefaultServiceRegistry
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList

class DefaultModuleManagerTest {

    companion object {

        val LOG = CopyOnWriteArrayList<String>()
        var failingModuleClosed = false
        var modeA = TransactionMode.SUCCESS
        var modeB = TransactionMode.SUCCESS
        var prepareGateA: CompletableFuture<Void>? = null
        var prepareGateB: CompletableFuture<Void>? = null

    }

    private val schedulers = mutableListOf<DefaultScheduler>()

    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun resetState() {
        LOG.clear()
        failingModuleClosed = false
        modeA = TransactionMode.SUCCESS
        modeB = TransactionMode.SUCCESS
        prepareGateA = null
        prepareGateB = null
    }

    @AfterEach
    fun closeSchedulers() {
        schedulers.forEach(DefaultScheduler::close)
    }

    @Test
    fun `reload when dependencies change loads and unloads in correct order`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("dependency"),
                name = "Dependency",
                description = "Dependency",
                enabledByDefault = true,
                factory = ::DependencyModule,
            )
            feature(
                key = ModuleKey("dependent"),
                name = "Dependent",
                description = "Dependent",
                enabledByDefault = false,
                factory = ::DependentModule,
            ) {
                requires(ModuleKey("dependency"))
            }
        }

        val fixture = fixture(catalog)
        fixture.manager.start()
        assertEquals(listOf("load:dependency"), LOG.toList())

        LOG.clear()
        fixture.modules().modules["dependent"] = true
        reload(fixture)
        assertEquals(listOf("load:dependent"), LOG.toList())

        LOG.clear()
        fixture.modules().modules["dependent"] = false
        fixture.modules().modules["dependency"] = false
        reload(fixture)

        assertEquals(listOf("unload:dependent", "unload:dependency"), LOG.toList())
        assertFalse(fixture.manager.moduleEnabled("dependency"))
        assertFalse(fixture.manager.moduleEnabled("dependent"))
    }

    @Test
    fun `reload when prepare fails rolls back prepared transactions in reverse order`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("a"),
                name = "A",
                description = "A",
                enabledByDefault = true,
                factory = ::TransactionModuleA,
            )
            feature(
                key = ModuleKey("b"),
                name = "B",
                description = "B",
                enabledByDefault = true,
                factory = ::TransactionModuleB,
            ) {
                requires(ModuleKey("a"))
            }
        }

        val fixture = fixture(catalog)
        fixture.manager.start()
        modeB = TransactionMode.PREPARE_FAILURE
        LOG.clear()

        val ex = assertThrows<Exception> {
            reload(fixture)
        }
        assertTrue(
            ex.message!!.contains("prepare:b")
                    || (ex.cause?.message?.contains("prepare:b") == true)
        )
        assertEquals(
            listOf("prepare:a", "prepare:b", "rollback:a"),
            LOG.toList()
        )
    }

    @Test
    fun `reload when commit fails rolls back committed in reverse order`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("a"),
                name = "A",
                description = "A",
                enabledByDefault = true,
                factory = ::TransactionModuleA,
            )
            feature(
                key = ModuleKey("b"),
                name = "B",
                description = "B",
                enabledByDefault = true,
                factory = ::TransactionModuleB,
            ) {
                requires(ModuleKey("a"))
            }
        }

        val fixture = fixture(catalog)
        fixture.manager.start()
        modeB = TransactionMode.COMMIT_FAILURE
        LOG.clear()

        val ex = assertThrows<Exception> {
            reload(fixture)
        }
        assertTrue(
            ex.message!!.contains("commit:b")
                    || (ex.cause?.message?.contains("commit:b") == true)
        )
        assertEquals(
            listOf(
                "prepare:a",
                "prepare:b",
                "commit:a",
                "commit:b",
                "rollback:b",
                "rollback:a"
            ), LOG.toList()
        )
    }

    @Test
    fun `load when scope registration fails closes all side effects and unwinds scope`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("failing"),
                name = "Failing",
                description = "Failing",
                enabledByDefault = true,
                factory = ::FailingModule,
            )
        }

        val fixture = fixture(catalog)
        assertThrows<ModuleLoadException> {
            fixture.manager.start()
        }

        assertTrue(
            failingModuleClosed,
            "Owned service must be drained/closed when startup fails"
        )
        assertFalse(fixture.manager.moduleEnabled("failing"))
        assertEquals(
            ModuleLifecycleState.FAILED.name,
            fixture.manager.modules().first { it.id() == "failing" }.state()
        )
    }

    @Test
    fun `initial startup failure unwinds previously started modules in reverse order`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("a"),
                name = "A",
                description = "A",
                priority = 0,
                enabledByDefault = true,
                factory = ::DependencyModule,
            )
            feature(
                key = ModuleKey("b"),
                name = "B",
                description = "B",
                priority = 10,
                enabledByDefault = true,
                factory = ::FailingModule,
            ) {
                requires(ModuleKey("a"))
            }
        }

        val fixture = fixture(catalog)
        val ex = assertThrows<ModuleLoadException> {
            fixture.manager.start()
        }

        assertTrue(ex.message!!.contains("register-commands"))
        assertTrue(failingModuleClosed, "Failing module B must be closed")
        assertEquals(
            listOf("load:a", "unload:a"),
            LOG.toList(),
            "Module A must be unwound after B fails"
        )
        assertEquals(0, fixture.manager.activeCount())
        assertFalse(fixture.manager.moduleEnabled("a"))
        assertFalse(fixture.manager.moduleEnabled("b"))
    }

    @Test
    fun `factory failure does not leak scope and marks module failed`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("broken_factory"),
                name = "Broken Factory",
                description = "Broken",
                enabledByDefault = true,
                factory = { throw IllegalStateException("Factory constructor explosion") },
            )
        }

        val fixture = fixture(catalog)
        val ex = assertThrows<ModuleLoadException> {
            fixture.manager.start()
        }
        assertTrue(ex.message!!.contains("factory failed"))
        assertFalse(fixture.manager.moduleEnabled("broken_factory"))
        assertEquals(0, fixture.manager.activeCount())
    }

    @Test
    fun `enable disable and re-enable creates fresh module instances and scopes`() = runTest {
        var instanceCount = 0

        class CountingModule : CellulosesZModule {

            val instanceId = ++instanceCount
            override fun construct(context: ModuleContext) {
                LOG.add("construct:$instanceId")
            }

            override fun onUnload(context: ModuleContext) {
                LOG.add("unload:$instanceId")
            }

        }

        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("counting"),
                name = "Counting",
                description = "Counting",
                enabledByDefault = true,
                factory = { CountingModule() },
            )
        }

        val fixture = fixture(catalog)
        fixture.manager.start()
        assertEquals(listOf("construct:1"), LOG.toList())
        assertTrue(fixture.manager.moduleEnabled("counting"))

        // Disable
        fixture.manager.reconcile(emptySet())
        assertEquals(listOf("construct:1", "unload:1"), LOG.toList())
        assertFalse(fixture.manager.moduleEnabled("counting"))

        // Re-enable
        fixture.manager.reconcile(setOf(ModuleKey("counting")))
        assertEquals(listOf("construct:1", "unload:1", "construct:2"), LOG.toList())
        assertTrue(fixture.manager.moduleEnabled("counting"))
    }

    @Test
    fun `optional dependency availability change restarts consumer module`() = runTest {
        class ProviderModule : CellulosesZModule {

            override fun construct(context: ModuleContext) {
                LOG.add("construct:provider")
            }

            override fun onUnload(context: ModuleContext) {
                LOG.add("unload:provider")
            }

        }

        class ConsumerModule : CellulosesZModule {

            override fun construct(context: ModuleContext) {
                val providerActive = context.moduleEnabled("provider")
                LOG.add("construct:consumer:withProvider=$providerActive")
            }

            override fun onUnload(context: ModuleContext) {
                LOG.add("unload:consumer")
            }

        }

        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("provider"),
                name = "Provider",
                description = "Provider",
                enabledByDefault = false,
                factory = { ProviderModule() },
            )
            feature(
                key = ModuleKey("consumer"),
                name = "Consumer",
                description = "Consumer",
                enabledByDefault = true,
                factory = { ConsumerModule() },
            ) {
                optional(ModuleKey("provider"))
            }
        }

        val fixture = fixture(catalog)
        fixture.manager.start()
        assertEquals(listOf("construct:consumer:withProvider=false"), LOG.toList())

        // Enable optional provider -> consumer restarts to pick up provider
        LOG.clear()
        fixture.manager.reconcile(setOf(ModuleKey("provider"), ModuleKey("consumer")))
        assertEquals(
            listOf(
                "unload:consumer",
                "construct:provider",
                "construct:consumer:withProvider=true"
            ), LOG.toList()
        )

        // Disable optional provider -> consumer restarts to drop provider
        LOG.clear()
        fixture.manager.reconcile(setOf(ModuleKey("consumer")))
        assertEquals(
            listOf(
                "unload:consumer",
                "unload:provider",
                "construct:consumer:withProvider=false"
            ), LOG.toList()
        )
    }

    @Test
    fun `server lifecycle catch-up hooks execute in order for late enabled module`() = runTest {
        class LateModule : CellulosesZModule {

            override fun construct(context: ModuleContext) {
                LOG.add("construct")
            }

            override fun onServerStarting(context: ModuleContext) {
                LOG.add("server-starting")
            }

            override fun onServerStarted(context: ModuleContext) {
                LOG.add("server-started")
            }

        }

        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("late"),
                name = "Late",
                description = "Late",
                enabledByDefault = false,
                factory = { LateModule() },
            )
        }

        val fixture = fixture(catalog)
        fixture.manager.start()
        fixture.manager.onServerStarting()
        fixture.manager.onServerStarted()

        LOG.clear()
        fixture.manager.reconcile(setOf(ModuleKey("late")))
        assertEquals(listOf("construct", "server-starting", "server-started"), LOG.toList())
        assertTrue(fixture.manager.moduleEnabled("late"))
    }

    @Test
    fun `suspend gated startup suspends and resumes cleanly`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val initStarted = CompletableDeferred<Unit>()

        class GatedModule : CellulosesZModule {

            override fun registerServices(context: ModuleContext) {
                context.services().register(
                    AsyncInitializable::class.java,
                    AsyncInitializable {
                        val future = CompletableFuture<Void>()
                        CoroutineScope(Dispatchers.Default).launch {
                            initStarted.complete(Unit)
                            gate.await()
                            future.complete(null)
                        }
                        future
                    }
                )
            }

        }

        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("gated"),
                name = "Gated",
                description = "Gated",
                enabledByDefault = true,
                factory = { GatedModule() },
            )
        }

        val fixture = fixture(catalog)
        val startJob = launch(Dispatchers.Default) {
            fixture.manager.start()
        }

        // Wait until module enters async initialization
        initStarted.await()

        assertFalse(fixture.manager.moduleEnabled("gated"))
        assertEquals(DefaultModuleManager.ManagerState.STARTING, fixture.manager.state)

        gate.complete(Unit)
        startJob.join()

        assertTrue(fixture.manager.moduleEnabled("gated"))
        assertEquals(DefaultModuleManager.ManagerState.RUNNING, fixture.manager.state)
    }

    @Test
    fun `cancellation during start cleans up module scope without leaking`() = runTest {
        val startGate = CompletableDeferred<Unit>()
        val initStarted = CompletableDeferred<Unit>()

        class SuspendingModule : CellulosesZModule {

            override fun registerServices(context: ModuleContext) {
                context.services().register(
                    AsyncInitializable::class.java,
                    AsyncInitializable {
                        val future = CompletableFuture<Void>()
                        CoroutineScope(Dispatchers.Default).launch {
                            initStarted.complete(Unit)
                            startGate.await()
                            future.complete(null)
                        }
                        future
                    }
                )
                context.scope().own(MarkerService())
            }

        }

        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("suspending"),
                name = "Suspending",
                description = "Suspending",
                enabledByDefault = true,
                factory = { SuspendingModule() },
            )
        }

        val fixture = fixture(catalog)
        val job = launch(Dispatchers.Default) {
            fixture.manager.start()
        }

        initStarted.await()
        job.cancelAndJoin()

        assertTrue(
            failingModuleClosed,
            "Service in cancelled module scope must be drained/closed"
        )
        assertFalse(fixture.manager.moduleEnabled("suspending"))
    }

    @Test
    fun `concurrent manager mutations are safely serialized`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("a"),
                name = "A",
                description = "A",
                enabledByDefault = true,
                factory = ::DependencyModule,
            )
            feature(
                key = ModuleKey("b"),
                name = "B",
                description = "B",
                enabledByDefault = true,
                factory = ::DependentModule,
            ) {
                requires(ModuleKey("a"))
            }
        }

        val fixture = fixture(catalog)
        fixture.manager.start()

        // Launch concurrent reconcile calls
        val job1 = launch {
            fixture.manager.reconcile(setOf(ModuleKey("a")))
        }
        val job2 = launch {
            fixture.manager.reconcile(
                setOf(
                    ModuleKey("a"),
                    ModuleKey("b")
                )
            )
        }

        job1.join()
        job2.join()

        // State is consistent
        val active = fixture.manager.activeKeys()
        assertTrue(active.contains(ModuleKey("a")))
    }

    @Test
    fun `stop is idempotent and prevents further reconciliation`() = runTest {
        val catalog = moduleCatalog {
            feature(
                key = ModuleKey("a"),
                name = "A",
                description = "A",
                enabledByDefault = true,
                factory = ::DependencyModule,
            )
        }

        val fixture = fixture(catalog)
        fixture.manager.start()
        assertTrue(fixture.manager.moduleEnabled("a"))

        fixture.manager.stop()
        assertFalse(fixture.manager.moduleEnabled("a"))
        assertEquals(
            DefaultModuleManager.ManagerState.STOPPED,
            fixture.manager.state
        )

        // Second stop is safe no-op
        fixture.manager.stop()
        assertEquals(
            DefaultModuleManager.ManagerState.STOPPED,
            fixture.manager.state
        )

        // Reconcile after stop throws
        assertThrows<IllegalStateException> {
            fixture.manager.reconcile(setOf(ModuleKey("a")))
        }
    }

    private fun fixture(catalog: ModuleCatalog): Fixture {
        val logger = NoopLogger()
        val services = DefaultServiceRegistry()
        val configs = JacksonConfigRegistry(
            root.resolve("config-${schedulers.size}"),
            logger,
        )
        val scheduler = DefaultScheduler(logger)
        val runtime = CellulosesRuntime(logger)

        schedulers.add(scheduler)
        val manager = DefaultModuleManager(
            catalog = catalog,
            dataDirectory = root.resolve("data-${schedulers.size}"),
            services = services,
            configs = configs,
            events = SimpleEventRegistry(),
            scheduler = scheduler,
            middlewares = DefaultCommandExecutionPipeline(logger, services),
            logger = logger,
            runtime = runtime,
        )

        return Fixture(manager, configs, services)
    }

    private suspend fun reload(fixture: Fixture) {
        val prepared = fixture.manager.prepareReload(
            fixture.configs.snapshot()
        ).await()
        prepared.commit().await()
    }

    private class Fixture(
        val manager: DefaultModuleManager,
        val configs: JacksonConfigRegistry,
        val services: DefaultServiceRegistry,
    ) {

        fun modules(): ModulesConfig = configs.require(
            "modules",
            ModulesConfig::class.java
        )

    }

    enum class TransactionMode {

        SUCCESS,
        PREPARE_FAILURE,
        COMMIT_FAILURE,
        ROLLBACK_FAILURE,

    }

    class DependencyModule : CellulosesZModule {

        override fun construct(context: ModuleContext) {
            LOG.add("load:${context.moduleId()}")
        }

        override fun onUnload(context: ModuleContext) {
            LOG.add("unload:${context.moduleId()}")
        }

    }

    class DependentModule : CellulosesZModule {

        override fun construct(context: ModuleContext) {
            LOG.add("load:${context.moduleId()}")
        }

        override fun onUnload(context: ModuleContext) {
            LOG.add("unload:${context.moduleId()}")
        }

    }

    class FailingModule : CellulosesZModule {

        override fun registerServices(context: ModuleContext) {
            context.scope().own(MarkerService())
        }

        override fun registerCommands(context: ModuleContext) {
            throw IllegalStateException("Failed during register-commands")
        }

    }

    class TransactionModuleA : TransactionModule("a") {

        override fun mode(): TransactionMode = modeA
        override fun prepareGate(): CompletableFuture<Void>? = prepareGateA

    }

    class TransactionModuleB : TransactionModule("b") {

        override fun mode(): TransactionMode = modeB
        override fun prepareGate(): CompletableFuture<Void>? = prepareGateB

    }

    abstract class TransactionModule(private val id: String) : CellulosesZModule {

        override fun prepareReload(context: ModuleReloadContext): CompletionStage<PreparedModuleReload> {
            LOG.add("prepare:$id")
            if (mode() == TransactionMode.PREPARE_FAILURE) {
                return CompletableFuture.failedFuture(IllegalStateException("prepare:$id"))
            }

            val gate = prepareGate()
            val ready = gate ?: CompletableFuture.completedFuture(null)
            return ready.thenApply {
                PreparedReloads.of(
                    {
                        LOG.add("commit:$id")
                        if (mode() == TransactionMode.COMMIT_FAILURE) {
                            CompletableFuture.failedFuture(IllegalStateException("commit:$id"))
                        } else {
                            CompletableFuture.completedFuture(null)
                        }
                    },
                    {
                        LOG.add("rollback:$id")
                        if (mode() == TransactionMode.ROLLBACK_FAILURE) {
                            CompletableFuture.failedFuture(IllegalStateException("rollback:$id"))
                        } else {
                            CompletableFuture.completedFuture(null)
                        }
                    }
                )
            }
        }

        abstract fun mode(): TransactionMode
        abstract fun prepareGate(): CompletableFuture<Void>?

    }

    class MarkerService : AsyncCloseable {

        override fun stopAccepting() {}
        override fun drain(): CompletableFuture<Void> {
            failingModuleClosed = true
            return CompletableFuture.completedFuture(null)
        }

    }

    private class NoopLogger : CellulosesZLogger {

        override fun warn(message: String) = Unit
        override fun error(message: String) = Unit
        override fun error(message: String, throwable: Throwable) = Unit
        override fun info(message: String) = Unit

    }

}
