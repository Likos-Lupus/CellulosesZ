package top.likoslupus.cellulosesz.core.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class KeyedMutexTest {

    @Test
    fun `same key operations never overlap`() = runTest {
        val mutex = KeyedMutex<String>()
        val activeCount = AtomicInteger(0)
        val maxOverlapping = AtomicInteger(0)
        val totalOps = 50

        val jobs = (1..totalOps).map {
            async(Dispatchers.Default) {
                mutex.withLock("same-key") {
                    val current = activeCount.incrementAndGet()
                    maxOverlapping.updateAndGet { prev -> maxOf(prev, current) }
                    delay(2.milliseconds)
                    activeCount.decrementAndGet()
                }
            }
        }

        jobs.awaitAll()
        assertEquals(
            1,
            maxOverlapping.get(),
            "Same-key operations should never have active count > 1"
        )
        assertEquals(0, activeCount.get())
        assertEquals(1, mutex.keyCount)
    }

    @Test
    fun `different key operations execute concurrently`() = runTest {
        val mutex = KeyedMutex<String>()
        val keyAHeld = CompletableDeferred<Unit>()
        val releaseKeyA = CompletableDeferred<Unit>()
        val keyBCompleted = AtomicBoolean(false)

        val jobA = async {
            mutex.withLock("key-A") {
                keyAHeld.complete(Unit)
                releaseKeyA.await()
            }
        }

        keyAHeld.await()

        // Key B should execute without being blocked by Key A
        val jobB = async {
            mutex.withLock("key-B") {
                keyBCompleted.set(true)
            }
        }

        jobB.await()
        assertTrue(
            keyBCompleted.get(),
            "Key B should execute concurrently while Key A is held"
        )

        releaseKeyA.complete(Unit)
        jobA.await()
        assertEquals(2, mutex.keyCount)
    }

    @Test
    fun `failure does not poison key`() = runTest {
        val mutex = KeyedMutex<String>()

        try {
            mutex.withLock("key-fail") {
                throw IllegalStateException("Failure in critical section")
            }
        } catch (_: IllegalStateException) {
            // expected
        }

        // Next operation on same key should succeed
        val nextSuccess = mutex.withLock("key-fail") {
            "recovered"
        }
        assertEquals("recovered", nextSuccess)
    }

    @Test
    fun `cancellation releases key for subsequent callers`() = runTest {
        val mutex = KeyedMutex<String>()
        val holderStarted = CompletableDeferred<Unit>()

        val holderJob = launch {
            mutex.withLock("cancel-key") {
                holderStarted.complete(Unit)
                awaitCancellation()
            }
        }

        holderStarted.await()

        // Cancel holder
        holderJob.cancel()
        holderJob.join()

        // Next caller should acquire without being blocked
        val result = mutex.withLock("cancel-key") {
            "acquired-after-cancel"
        }
        assertEquals("acquired-after-cancel", result)
    }

    @Test
    fun `multi-key lock ordering prevents deadlocks between opposite-order callers`() = runTest {
        val mutex = KeyedMutex<String>()
        val iterations = 50
        val successCount = AtomicInteger(0)

        // Launch concurrent operations requesting [A, B] and [B, A] in opposite order
        val jobs = (1..iterations).flatMap {
            listOf(
                async(Dispatchers.Default) {
                    mutex.withLocks(listOf("account-A", "account-B")) {
                        delay(1.milliseconds)
                        successCount.incrementAndGet()
                    }
                },
                async(Dispatchers.Default) {
                    mutex.withLocks(listOf("account-B", "account-A")) {
                        delay(1.milliseconds)
                        successCount.incrementAndGet()
                    }
                }
            )
        }

        jobs.awaitAll()
        assertEquals(
            iterations * 2,
            successCount.get(),
            "All multi-key operations should complete without deadlock"
        )
    }

    @Test
    fun `withLocks handles empty and single key collections`() = runTest {
        val mutex = KeyedMutex<String>()

        val emptyResult = mutex.withLocks(emptyList()) { "empty" }
        assertEquals("empty", emptyResult)

        val singleResult = mutex.withLocks(listOf("single")) { "single" }
        assertEquals("single", singleResult)
        assertEquals(1, mutex.keyCount)

        val duplicatesResult = mutex.withLocks(listOf("dup", "dup", "dup")) { "deduped" }
        assertEquals("deduped", duplicatesResult)
        assertEquals(2, mutex.keyCount)
    }

}
