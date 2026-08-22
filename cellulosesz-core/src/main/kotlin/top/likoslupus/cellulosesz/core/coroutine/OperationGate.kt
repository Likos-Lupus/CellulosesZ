package top.likoslupus.cellulosesz.core.coroutine

import kotlinx.coroutines.CompletableDeferred
import top.likoslupus.cellulosesz.core.lifecycle.DrainableResource
import top.likoslupus.cellulosesz.core.lifecycle.LifecycleClosedException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * A concurrency primitive separating operation acceptance from execution and drain.
 *
 * Guarantees:
 * - Acceptance is decided synchronously and atomically upon entry.
 * - Once accepted, an operation is allowed to complete even if [stopAccepting] is subsequently called.
 * - Calling [stopAccepting] causes subsequent [withAcceptedOperation] calls to immediately fail with [LifecycleClosedException].
 * - Calling [drain] suspends until all in-flight accepted operations complete, fail, or cancel.
 * - Completion, failure, or cancellation of accepted operations safely decrements the in-flight counter.
 * - Repeated calls to [stopAccepting] or [drain] are idempotent and race-free.
 */
class OperationGate(
    val name: String = "operation-gate",
) : DrainableResource {

    private val inFlight = AtomicInteger(0)
    private val isAccepting = AtomicBoolean(true)
    private val drainSignal = AtomicReference<CompletableDeferred<Unit>?>(null)

    val accepting: Boolean
        get() = isAccepting.get()

    val activeCount: Int
        get() = inFlight.get()

    /**
     * Executes the given [block] within an accepted operation boundary.
     *
     * @throws LifecycleClosedException if this gate has stopped accepting new operations.
     */
    suspend fun <T> withAcceptedOperation(block: suspend () -> T): T {
        enter()
        return try {
            block()
        } finally {
            leave()
        }
    }

    private fun enter() {
        synchronized(this) {
            if (!isAccepting.get()) {
                throw LifecycleClosedException("Operation gate '$name' is not accepting new operations")
            }
            inFlight.incrementAndGet()
        }
    }

    private fun leave() {
        val remaining = inFlight.decrementAndGet()
        if (remaining == 0 && !isAccepting.get()) {
            synchronized(this) {
                drainSignal.get()?.complete(Unit)
            }
        }
    }

    override fun stopAccepting() {
        synchronized(this) {
            isAccepting.set(false)
            if (inFlight.get() == 0) {
                drainSignal.get()?.complete(Unit)
            }
        }
    }

    override suspend fun drain() {
        val signal = synchronized(this) {
            if (inFlight.get() == 0) {
                return
            }
            drainSignal.updateAndGet { it ?: CompletableDeferred() }!!
        }
        signal.await()
    }

}
