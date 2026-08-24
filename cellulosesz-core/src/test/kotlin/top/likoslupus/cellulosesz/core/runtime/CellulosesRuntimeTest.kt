package top.likoslupus.cellulosesz.core.runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.lifecycle.LifecycleClosedException
import top.likoslupus.cellulosesz.core.lifecycle.SuspendCloseable
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class CellulosesRuntimeTest {

    @Test
    fun `runtime initializes with running state and creates child module scopes`() = runTest {
        val logger = RecordingLogger()
        val runtime = CellulosesRuntime(logger)

        assertEquals(RuntimeState.RUNNING, runtime.state)
        assertTrue(runtime.isAccepting)

        val scopeA = runtime.createModuleScope("mod-a")
        val scopeB = runtime.createModuleScope("mod-b")

        assertEquals("mod-a", scopeA.owner())
        assertEquals("mod-b", scopeB.owner())
        assertTrue(scopeA.accepting)
        assertTrue(scopeB.accepting)

        runtime.shutdown()
        assertEquals(RuntimeState.STOPPED, runtime.state)
        assertFalse(runtime.isAccepting)
    }

    @Test
    fun `runtime supervision isolates sibling failures and logs uncaught exceptions`() = runTest {
        val logger = RecordingLogger()
        val runtime = CellulosesRuntime(logger)

        val siblingRunning = CompletableDeferred<Unit>()

        runtime.coroutineScope.launch {
            while (isActive) {
                delay(10.milliseconds)
                siblingRunning.complete(Unit)
            }
        }

        // Child failure should be caught by CoroutineExceptionHandler
        val failingJob = runtime.coroutineScope.launch {
            throw RuntimeException("Simulated crash in background job")
        }

        failingJob.join()
        siblingRunning.await()

        assertTrue(runtime.coroutineScope.isActive)
        assertEquals(1, logger.loggedErrors.size)
        assertTrue(logger.loggedErrors[0].contains("Uncaught coroutine exception"))

        runtime.shutdown()
    }

    @Test
    fun `cancellation is not logged as an error`() = runTest {
        val logger = RecordingLogger()
        val runtime = CellulosesRuntime(logger)

        val job = runtime.coroutineScope.launch {
            delay(1000.milliseconds)
        }

        job.cancel()
        job.join()

        assertEquals(0, logger.loggedErrors.size)
        runtime.shutdown()
    }

    @Test
    fun `shutdown closes child module scopes in reverse creation order`() = runTest {
        val logger = RecordingLogger()
        val runtime = CellulosesRuntime(logger)
        val closeOrder = CopyOnWriteArrayList<String>()

        val scope1 = runtime.createModuleScope("first")
        val scope2 = runtime.createModuleScope("second")
        val scope3 = runtime.createModuleScope("third")

        scope1.own(SuspendCloseable { closeOrder.add("first-closed") })
        scope2.own(SuspendCloseable { closeOrder.add("second-closed") })
        scope3.own(SuspendCloseable { closeOrder.add("third-closed") })

        runtime.shutdown()

        assertEquals(listOf("third-closed", "second-closed", "first-closed"), closeOrder)
        assertEquals(RuntimeState.STOPPED, runtime.state)
    }

    @Test
    fun `createModuleScope rejects creation after shutdown has begun`() = runTest {
        val logger = RecordingLogger()
        val runtime = CellulosesRuntime(logger)

        runtime.shutdown()

        assertThrows(LifecycleClosedException::class.java) {
            runtime.createModuleScope("late-module")
        }
    }

    @Test
    fun `shutdown is idempotent when invoked concurrently`() = runTest {
        val logger = RecordingLogger()
        val runtime = CellulosesRuntime(logger)
        val closeCount = AtomicInteger(0)

        val scope = runtime.createModuleScope("test-mod")
        scope.own(SuspendCloseable { closeCount.incrementAndGet() })

        val s1 = async { runtime.shutdown() }
        val s2 = async { runtime.shutdown() }
        val s3 = async { runtime.shutdown() }

        s1.await()
        s2.await()
        s3.await()

        assertEquals(1, closeCount.get())
        assertEquals(RuntimeState.STOPPED, runtime.state)
    }

    @Test
    fun `shutdown cancels and joins remaining root jobs`() = runTest {
        val logger = RecordingLogger()
        val runtime = CellulosesRuntime(logger)

        val jobStarted = CompletableDeferred<Unit>()
        val jobCancelled = CompletableDeferred<Unit>()

        runtime.coroutineScope.launch {
            jobStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                jobCancelled.complete(Unit)
            }
        }

        jobStarted.await()
        runtime.shutdown()

        assertTrue(jobCancelled.isCompleted)
    }

    private class RecordingLogger : CellulosesZLogger {

        val loggedErrors = CopyOnWriteArrayList<String>()
        val loggedWarns = CopyOnWriteArrayList<String>()
        val loggedInfos = CopyOnWriteArrayList<String>()

        override fun warn(message: String) {
            loggedWarns.add(message)
        }

        override fun error(message: String) {
            loggedErrors.add(message)
        }

        override fun error(message: String, throwable: Throwable) {
            loggedErrors.add("$message: ${throwable.message}")
        }

        override fun info(message: String) {
            loggedInfos.add(message)
        }

    }

}
