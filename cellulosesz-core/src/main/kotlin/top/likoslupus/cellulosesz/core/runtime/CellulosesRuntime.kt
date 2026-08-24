package top.likoslupus.cellulosesz.core.runtime

import kotlinx.coroutines.*
import top.likoslupus.cellulosesz.core.lifecycle.LifecycleClosedException
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger
import top.likoslupus.cellulosesz.core.module.DefaultModuleScope
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Root coroutine runtime for CellulosesZ.
 *
 * Owns the root [SupervisorJob], the top-level runtime [CoroutineScope], error supervision,
 * dispatcher management, and the lifecycle tree of child module scopes.
 */
class CellulosesRuntime @JvmOverloads constructor(
    val logger: CellulosesZLogger,
    val dispatchers: RuntimeDispatchers = RuntimeDispatchers(),
) {

    private val rootJob: CompletableJob = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            logger.error("Uncaught coroutine exception in runtime scope", throwable)
        }
    }

    val coroutineScope: CoroutineScope = CoroutineScope(
        rootJob +
                dispatchers.application +
                CoroutineName("cellulosesz-runtime") +
                exceptionHandler
    )

    private val stateRef = AtomicReference(RuntimeState.RUNNING)
    private val shutdownDeferred = AtomicReference<CompletableDeferred<Unit>?>(null)
    private val moduleScopes = Collections.synchronizedList(ArrayList<DefaultModuleScope>())

    val state: RuntimeState
        get() = stateRef.get()

    val isAccepting: Boolean
        get() = stateRef.get() == RuntimeState.RUNNING

    /**
     * Creates and attaches a new [DefaultModuleScope] as a child of this runtime.
     *
     * @throws LifecycleClosedException if the runtime is not in [RuntimeState.RUNNING].
     */
    fun createModuleScope(moduleId: String): DefaultModuleScope {
        synchronized(this) {
            if (stateRef.get() != RuntimeState.RUNNING) {
                throw LifecycleClosedException(
                    "Cannot create module scope for '$moduleId'; runtime is in state ${stateRef.get()}"
                )
            }

            val moduleScope = DefaultModuleScope(
                owner = moduleId,
                parentJob = rootJob,
                parentContext = dispatchers.application + exceptionHandler,
            )
            moduleScopes.add(moduleScope)
            return moduleScope
        }
    }

    /**
     * Initiates suspending, structured shutdown of the runtime.
     *
     * Shutdown ordering:
     * 1. Atomically transitions state to [RuntimeState.STOPPING], rejecting new module scopes.
     * 2. Signals all child module scopes to stop accepting new work.
     * 3. Closes/drains child module scopes in reverse creation order.
     * 4. Cancels and joins remaining root-owned jobs in the runtime scope.
     * 5. Transitions state to [RuntimeState.STOPPED].
     * 6. Aggregates and throws any teardown failures.
     */
    suspend fun shutdown() {
        val signal = synchronized(this) {
            if (stateRef.get() == RuntimeState.STOPPED) {
                return
            }

            val existing = shutdownDeferred.get()
            if (existing != null) {
                existing
            } else {
                stateRef.set(RuntimeState.STOPPING)
                val newSignal = CompletableDeferred<Unit>()
                shutdownDeferred.set(newSignal)
                null
            }
        }

        if (signal != null) {
            signal.await()
            return
        }

        val activeSignal = shutdownDeferred.get()!!
        val failures = mutableListOf<Throwable>()

        try {
            // Step 1: Tell all module scopes to stop accepting new work
            val snapshot = synchronized(this) { moduleScopes.toList() }
            snapshot.forEach { scope ->
                try {
                    scope.stopAccepting()
                } catch (t: Throwable) {
                    if (t !is CancellationException) {
                        failures.add(t)
                    }
                }
            }

            // Step 2: Close module scopes in reverse creation order
            snapshot.asReversed().forEach { scope ->
                try {
                    scope.close()
                } catch (t: Throwable) {
                    if (t !is CancellationException) {
                        failures.add(t)
                    }
                }
            }

            // Step 3: Cancel and join remaining root jobs (excluding the current teardown job)
            val currentJob = currentCoroutineContext()[Job]
            val remainingJobs = rootJob.children.filter { it != currentJob }.toList()
            remainingJobs.forEach { child ->
                child.cancel()
            }
            remainingJobs.forEach { child ->
                try {
                    child.join()
                } catch (t: Throwable) {
                    if (t !is CancellationException) {
                        failures.add(t)
                    }
                }
            }
            rootJob.complete()
        } finally {
            stateRef.set(RuntimeState.STOPPED)
            if (failures.isEmpty()) {
                activeSignal.complete(Unit)
            } else {
                val aggregate = IllegalStateException("CellulosesRuntime shutdown completed with ${failures.size} failure(s)")
                failures.forEach { aggregate.addSuppressed(it) }
                activeSignal.completeExceptionally(aggregate)
            }
        }

        if (failures.isNotEmpty()) {
            val aggregate = IllegalStateException("CellulosesRuntime shutdown completed with ${failures.size} failure(s)")
            failures.forEach { aggregate.addSuppressed(it) }
            throw aggregate
        }
    }

}
