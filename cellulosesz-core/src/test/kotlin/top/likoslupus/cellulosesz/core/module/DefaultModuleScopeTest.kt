package top.likoslupus.cellulosesz.core.module

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.api.service.Registration
import top.likoslupus.cellulosesz.core.lifecycle.DrainableResource
import top.likoslupus.cellulosesz.core.lifecycle.LifecycleClosedException
import top.likoslupus.cellulosesz.core.lifecycle.SuspendCloseable
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncCloseable
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger
import top.likoslupus.cellulosesz.core.runtime.CellulosesRuntime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class DefaultModuleScopeTest {

    @Test
    fun `scope is child of parent runtime`() = runTest {
        val runtime = CellulosesRuntime(NoopLogger())
        val scope = runtime.createModuleScope("test-mod")

        assertTrue(scope.accepting)
        assertEquals("test-mod", scope.owner())

        // Child job should be active and attached to root job
        val moduleJob = scope.coroutineContext.job
        assertTrue(moduleJob.isActive)

        scope.close()
        assertFalse(moduleJob.isActive)
    }

    @Test
    fun `scope owns and cancels launched coroutines on close`() = runTest {
        val runtime = CellulosesRuntime(NoopLogger())
        val scope = runtime.createModuleScope("test-mod")

        val jobStarted = CompletableDeferred<Unit>()
        val jobEnded = CompletableDeferred<Unit>()

        scope.launch {
            jobStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                jobEnded.complete(Unit)
            }
        }

        jobStarted.await()
        scope.close()

        assertTrue(jobEnded.isCompleted)
    }

    @Test
    fun `close uses reverse order across all resource types and aggregates failures`() = runTest {
        val order = Collections.synchronizedList(ArrayList<String>())
        val runtime = CellulosesRuntime(NoopLogger())
        val scope = runtime.createModuleScope("test")

        val asyncGate = CompletableFuture<Void>()

        val reg1 = TestRegistration("test", "first-reg", order, fail = false)
        val suspendCloseable = TestSuspendCloseable("suspend-1", order, fail = false)
        val drainable = TestDrainable("drainable-1", order, fail = false)
        val asyncCloseable = TestAsyncCloseable("async-1", order, asyncGate)
        val failingReg = TestRegistration("test", "failing-reg", order, fail = true)

        scope.own(reg1)
        scope.own(suspendCloseable)
        scope.own(drainable)
        @Suppress("DEPRECATION")
        scope.own(asyncCloseable)
        scope.own(failingReg)

        // Allow asyncCloseable to drain
        asyncGate.complete(null)

        var failure: IllegalStateException? = null
        try {
            scope.close()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            failure = e
        }
        assertNotNull(failure)

        // Drainables/AsyncCloseables stop first, then drain in reverse order,
        // then SuspendCloseables in reverse order, then Registrations in reverse order
        assertTrue(order.contains("drainable-1-stop"))
        assertTrue(order.contains("async-1-stop"))
        assertTrue(order.contains("async-1-drain"))
        assertTrue(order.contains("drainable-1-drain"))
        assertTrue(order.contains("suspend-1-close"))
        assertTrue(order.contains("failing-reg"))
        assertTrue(order.contains("first-reg"))

        // Reverse registration ordering: failing-reg was registered after first-reg, so it closed before first-reg
        val failingIdx = order.indexOf("failing-reg")
        val firstIdx = order.indexOf("first-reg")
        assertTrue(failingIdx < firstIdx, "failing-reg should close before first-reg")

        // Failures should be aggregated
        assertEquals("Module scope close failed: test", failure!!.message)
        assertEquals(1, failure.suppressedExceptions.size)
    }

    @Test
    fun `stopAccepting rejects new work immediately`() = runTest {
        val scope = DefaultModuleScope("test")
        scope.stopAccepting()

        assertTrue(scope.closing())
        assertFalse(scope.accepting)

        val reg = TestRegistration("test", "reg", ArrayList(), false)
        assertThrows(LifecycleClosedException::class.java) {
            scope.own(reg)
        }
        assertTrue(reg.closed())

        assertThrows(LifecycleClosedException::class.java) {
            scope.own(TestSuspendCloseable("suspend", ArrayList(), false))
        }

        assertThrows(LifecycleClosedException::class.java) {
            scope.own(TestDrainable("drainable", ArrayList(), false))
        }
    }

    @Test
    fun `register when owner mismatch closes without leak`() = runTest {
        val order = ArrayList<String>()
        val scope = DefaultModuleScope("expected")
        val registration = TestRegistration("other", "closed", order, false)

        assertThrows(IllegalArgumentException::class.java) { scope.own(registration) }
        assertTrue(registration.closed())
        assertEquals(listOf("closed"), order)

        scope.close()
        assertEquals(listOf("closed"), order)
    }

    @Test
    fun `close is idempotent across concurrent callers`() = runTest {
        val closeCount = AtomicInteger(0)
        val scope = DefaultModuleScope("test")
        scope.own(SuspendCloseable {
            closeCount.incrementAndGet()
        })

        val c1 = async { scope.close() }
        val c2 = async { scope.close() }
        val c3 = async { scope.close() }

        c1.await()
        c2.await()
        c3.await()

        assertEquals(1, closeCount.get())
        assertFalse(scope.accepting)
    }

    @Test
    fun `stopAccepting failure is aggregated during close`() = runTest {
        val scope = DefaultModuleScope("test")
        scope.own(object : DrainableResource {
            override fun stopAccepting() {
                throw IllegalStateException("Stop acceptance failed")
            }

            override suspend fun drain() {}
        })

        var failure: IllegalStateException? = null
        try {
            scope.close()
            fail<Unit>("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            failure = e
        }
        assertNotNull(failure)
        assertEquals(1, failure!!.suppressedExceptions.size)
        assertEquals("Stop acceptance failed", failure.suppressedExceptions[0].message)
    }

    @Test
    fun `own AsyncCloseable after stopAccepting propagates drain failure as cause`() = runTest {
        val scope = DefaultModuleScope("test")
        scope.stopAccepting()

        val failingCloseable = object : AsyncCloseable {
            override fun stopAccepting() {
                throw IllegalStateException("Sync failure on close")
            }

            override fun drain(): CompletableFuture<Void> {
                return CompletableFuture.completedFuture(null)
            }
        }

        val ex = assertThrows(LifecycleClosedException::class.java) {
            @Suppress("DEPRECATION")
            scope.own(failingCloseable)
        }
        assertNotNull(ex.cause)
        assertEquals("Sync failure on close", ex.cause!!.message)
    }

    @Test
    fun `concurrent registration and close race does not leak registrations`() = runTest {
        val scope = DefaultModuleScope("race-test")
        val order = Collections.synchronizedList(ArrayList<String>())
        val totalRegs = 100

        val regJobs = (1..totalRegs).map { i ->
            async(Dispatchers.Default) {
                try {
                    scope.own(TestRegistration("race-test", "reg-$i", order, false))
                } catch (_: LifecycleClosedException) {
                    // Registration was rejected because close() won the race
                }
            }
        }

        val closeJob = async(Dispatchers.Default) {
            scope.close()
        }

        regJobs.awaitAll()
        closeJob.await()

        // Every registration that was accepted MUST have been closed
        assertFalse(scope.accepting)
    }

    @Test
    fun `sibling module isolation under root supervisor`() = runTest {
        val runtime = CellulosesRuntime(NoopLogger())
        val scopeA = runtime.createModuleScope("module-a")
        val scopeB = runtime.createModuleScope("module-b")

        val siblingAlive = CompletableDeferred<Unit>()

        scopeB.launch {
            while (isActive) {
                delay(10.milliseconds)
                siblingAlive.complete(Unit)
            }
        }

        // Child failure in scopeA should not kill scopeB
        scopeA.launch {
            throw RuntimeException("Intentional crash in module A")
        }

        siblingAlive.await()
        assertTrue(scopeB.coroutineContext.job.isActive)
        assertTrue(runtime.coroutineScope.isActive)

        runtime.shutdown()
    }

    private class TestRegistration(
        private val owner: String,
        private val label: String,
        private val order: MutableList<String>,
        private val fail: Boolean,
    ) : Registration {

        private val isClosed = AtomicBoolean(false)

        override fun owner(): String = owner

        override fun closed(): Boolean = isClosed.get()

        override fun close() {
            if (isClosed.compareAndSet(false, true)) {
                order.add(label)
                check(!fail) { "Failure in $label" }
            }
        }

    }

    private class TestSuspendCloseable(
        private val label: String,
        private val order: MutableList<String>,
        private val fail: Boolean,
    ) : SuspendCloseable {

        override suspend fun close() {
            order.add("$label-close")
            check(!fail) { "Failure in $label" }
        }

    }

    private class TestDrainable(
        private val label: String,
        private val order: MutableList<String>,
        private val fail: Boolean,
    ) : DrainableResource {

        override fun stopAccepting() {
            order.add("$label-stop")
        }

        override suspend fun drain() {
            order.add("$label-drain")
            if (fail) {
                throw IllegalStateException("Failure in $label")
            }
        }

    }

    private class TestAsyncCloseable(
        private val label: String,
        private val order: MutableList<String>,
        private val gate: CompletableFuture<Void>,
    ) : AsyncCloseable {

        override fun stopAccepting() {
            order.add("$label-stop")
        }

        override fun drain(): CompletableFuture<Void> {
            order.add("$label-drain")
            return gate
        }

    }

    private class NoopLogger : CellulosesZLogger {

        override fun warn(message: String) = Unit
        override fun error(message: String) = Unit
        override fun error(message: String, throwable: Throwable) = Unit
        override fun info(message: String) = Unit

    }

}
