package top.likoslupus.cellulosesz.core.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.lifecycle.LifecycleClosedException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class OperationGateTest {

    @Test
    fun `withAcceptedOperation executes block when accepting`() = runTest {
        val gate = OperationGate("test-gate")
        assertTrue(gate.accepting)
        assertEquals(0, gate.activeCount)

        val result = gate.withAcceptedOperation {
            assertEquals(1, gate.activeCount)
            "success"
        }

        assertEquals("success", result)
        assertEquals(0, gate.activeCount)
    }

    @Test
    fun `stopAccepting rejects subsequent operations`() = runTest {
        val gate = OperationGate("test-gate")
        gate.stopAccepting()

        assertFalse(gate.accepting)
        try {
            gate.withAcceptedOperation {
                fail<Unit>("Should not execute")
            }
            fail("Expected LifecycleClosedException")
        } catch (_: LifecycleClosedException) {
            // expected
        }
    }

    @Test
    fun `drain waits for accepted in-flight operations`() = runTest {
        val gate = OperationGate("test-gate")
        val operationStarted = CompletableDeferred<Unit>()
        val allowOperationToFinish = CompletableDeferred<Unit>()
        val operationFinished = AtomicBoolean(false)

        val op = async(Dispatchers.Default) {
            gate.withAcceptedOperation {
                operationStarted.complete(Unit)
                allowOperationToFinish.await()
                operationFinished.set(true)
            }
        }

        operationStarted.await()
        assertEquals(1, gate.activeCount)

        // Begin drain in background
        val drainCompleted = AtomicBoolean(false)
        val drainJob = async(Dispatchers.Default) {
            gate.stopAccepting()
            gate.drain()
            drainCompleted.set(true)
        }

        // Ensure post-stop operations are rejected
        try {
            gate.withAcceptedOperation { }
            fail("Expected LifecycleClosedException")
        } catch (_: LifecycleClosedException) {
            // expected
        }

        // Drain should not be complete yet because op is still in flight
        assertFalse(drainCompleted.get())
        assertFalse(operationFinished.get())

        // Allow in-flight operation to finish
        allowOperationToFinish.complete(Unit)
        op.await()
        drainJob.await()

        assertTrue(operationFinished.get())
        assertTrue(drainCompleted.get())
        assertEquals(0, gate.activeCount)
    }

    @Test
    fun `failed accepted operation decrements counter and unblocks drain`() = runTest {
        val gate = OperationGate("test-gate")
        val operationStarted = CompletableDeferred<Unit>()
        val allowFailure = CompletableDeferred<Unit>()

        supervisorScope {
            val op = async {
                gate.withAcceptedOperation {
                    operationStarted.complete(Unit)
                    allowFailure.await()
                    throw IllegalStateException("Intentional operation failure")
                }
            }

            operationStarted.await()

            val drainJob = async {
                gate.stopAccepting()
                gate.drain()
            }

            allowFailure.complete(Unit)
            try {
                op.await()
            } catch (_: IllegalStateException) {
                // expected
            }

            drainJob.await()
            assertEquals(0, gate.activeCount)
        }
    }

    @Test
    fun `cancelled accepted operation decrements counter and unblocks drain`() = runTest {
        val gate = OperationGate("test-gate")
        val operationStarted = CompletableDeferred<Unit>()

        val op = launch {
            gate.withAcceptedOperation {
                operationStarted.complete(Unit)
                awaitCancellation()
            }
        }

        operationStarted.await()
        assertEquals(1, gate.activeCount)

        val drainJob = async {
            gate.stopAccepting()
            gate.drain()
        }

        // Cancel the in-flight operation
        op.cancel()
        op.join()

        drainJob.await()
        assertEquals(0, gate.activeCount)
    }

    @Test
    fun `stress test concurrent operations and drain`() = runTest {
        val gate = OperationGate("stress-gate")
        val totalOps = 50
        val completedOps = AtomicInteger(0)

        val jobs = (1..totalOps).map {
            async(Dispatchers.Default) {
                try {
                    gate.withAcceptedOperation {
                        delay(5.milliseconds)
                        completedOps.incrementAndGet()
                    }
                } catch (_: LifecycleClosedException) {
                    // Rejected after stopAccepting
                }
            }
        }

        delay(10.milliseconds)
        gate.stopAccepting()
        gate.drain()

        jobs.awaitAll()
        assertEquals(0, gate.activeCount)
        assertTrue(completedOps.get() > 0)
    }

}
