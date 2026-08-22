package top.likoslupus.cellulosesz.core.module

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable
import top.likoslupus.cellulosesz.api.module.ModuleScope
import top.likoslupus.cellulosesz.api.service.Registration
import top.likoslupus.cellulosesz.core.legacy.LegacyFutureLifecycleAdapter
import top.likoslupus.cellulosesz.core.lifecycle.DrainableResource
import top.likoslupus.cellulosesz.core.lifecycle.LifecycleClosedException
import top.likoslupus.cellulosesz.core.lifecycle.SuspendCloseable
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine-native implementation of [ModuleScope].
 *
 * Owns:
 * - Coroutine jobs launched in this scope (parented under a child [SupervisorJob]).
 * - Synchronous [Registration] handles.
 * - Suspending [SuspendCloseable] resources.
 * - Two-phase [DrainableResource] resources.
 * - Legacy [AsyncCloseable] services (adapted).
 *
 * Shutdown Algorithm:
 * 1. Atomically transitions to stopping state ([isAccepting] = false).
 * 2. Signals all owned [DrainableResource]s and [AsyncCloseable]s to [stopAccepting].
 * 3. Drains in-flight operations of [DrainableResource]s and [AsyncCloseable]s in reverse registration order.
 * 4. Cancels and joins all active child coroutine jobs owned by this scope.
 * 5. Closes all [SuspendCloseable] resources in reverse registration order.
 * 6. Closes all synchronous [Registration] handles in reverse registration order.
 * 7. Completes the module job.
 * 8. Aggregates all teardown failures into a single exception with suppressed causes.
 */
class DefaultModuleScope(
    private val owner: String,
    parentJob: Job? = null,
    private val parentContext: CoroutineContext = Dispatchers.Default,
) : ModuleScope, CoroutineScope, SuspendCloseable, DrainableResource {

    init {
        require(owner.isNotBlank()) { "Module scope owner must not be blank" }
    }

    private val job: CompletableJob = SupervisorJob(parentJob)

    override val coroutineContext: CoroutineContext =
        parentContext + job + CoroutineName("module:$owner")

    private val isAccepting = AtomicBoolean(true)
    private val closeDeferred = AtomicReference<CompletableDeferred<Unit>?>(null)

    private val resources = Collections.synchronizedList(ArrayList<Any>())
    private val uniqueResources = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

    override fun owner(): String = owner

    override fun closing(): Boolean = !isAccepting.get()

    val accepting: Boolean
        get() = isAccepting.get()

    override fun <R : Registration> own(registration: R): R {
        if (owner != registration.owner()) {
            registration.close()
            throw IllegalArgumentException(
                "Registration owner ${registration.owner()} does not match module scope owner $owner"
            )
        }

        if (!isAccepting.get()) {
            registration.close()
            throw LifecycleClosedException("Module scope '$owner' is closing")
        }

        resources.add(registration)
        return registration
    }

    fun <T : SuspendCloseable> own(closeable: T): T {
        if (!isAccepting.get()) {
            throw LifecycleClosedException("Module scope '$owner' is closing")
        }

        if (uniqueResources.add(closeable)) {
            resources.add(closeable)
        }

        return closeable
    }

    fun <T : DrainableResource> own(drainable: T): T {
        if (!isAccepting.get()) {
            drainable.stopAccepting()
            throw LifecycleClosedException("Module scope '$owner' is closing")
        }

        if (uniqueResources.add(drainable)) {
            resources.add(drainable)
        }

        return drainable
    }

    fun own(closeable: AsyncCloseable) {
        if (!isAccepting.get()) {
            closeable.stopAccepting()
            closeable.drain()
            throw LifecycleClosedException("Module scope '$owner' is closing")
        }

        if (uniqueResources.add(closeable)) {
            resources.add(closeable)
        }
    }

    fun own(job: Job): Job {
        if (!isAccepting.get()) {
            job.cancel()
            throw LifecycleClosedException("Module scope '$owner' is closing")
        }

        return job
    }

    override fun stopAccepting() {
        if (isAccepting.compareAndSet(true, false)) {
            val snapshot = synchronized(resources) { resources.toList() }
            for (resource in snapshot) {
                try {
                    when (resource) {
                        is DrainableResource -> resource.stopAccepting()
                        is AsyncCloseable -> resource.stopAccepting()
                    }
                } catch (_: Throwable) {
                    // Stop acceptance errors should not prevent other resources from stopping
                }
            }
        }
    }

    override suspend fun drain() {
        stopAccepting()
        val snapshot = synchronized(resources) { resources.toList() }
        for (resource in snapshot.asReversed()) {
            when (resource) {
                is DrainableResource -> resource.drain()
                is AsyncCloseable -> resource.drain().await()
            }
        }
    }

    override suspend fun close() {
        val signal = synchronized(this) {
            val existing = closeDeferred.get()
            if (existing != null) {
                existing
            } else {
                val newSignal = CompletableDeferred<Unit>()
                closeDeferred.set(newSignal)
                null
            }
        }

        if (signal != null) {
            signal.await()
            return
        }

        val activeSignal = closeDeferred.get()!!
        val failures = mutableListOf<Throwable>()

        try {
            stopAccepting()

            val snapshot = synchronized(resources) { resources.toList() }
            val reverse = snapshot.asReversed()

            // 1. Drain DrainableResources / AsyncCloseables in reverse order
            for (resource in reverse) {
                try {
                    when (resource) {
                        is DrainableResource -> resource.drain()
                        is AsyncCloseable -> resource.drain().await()
                    }
                } catch (t: Throwable) {
                    if (t !is CancellationException) {
                        failures.add(t)
                    }
                }
            }

            // 2. Cancel and join all child jobs of this module scope (excluding the current teardown job)
            val currentJob = currentCoroutineContext()[Job]
            val childJobs = job.children.filter { it != currentJob }.toList()
            childJobs.forEach { it.cancel() }
            childJobs.forEach { child ->
                try {
                    child.join()
                } catch (t: Throwable) {
                    if (t !is CancellationException) {
                        failures.add(t)
                    }
                }
            }

            // 3. Close SuspendCloseables in reverse order
            for (resource in reverse) {
                if (resource is SuspendCloseable) {
                    try {
                        resource.close()
                    } catch (t: Throwable) {
                        if (t !is CancellationException) {
                            failures.add(t)
                        }
                    }
                }
            }

            // 4. Close synchronous Registrations in reverse order
            for (resource in reverse) {
                if (resource is Registration) {
                    try {
                        resource.close()
                    } catch (t: Throwable) {
                        failures.add(t)
                    }
                }
            }

            job.complete()
        } finally {
            if (failures.isEmpty()) {
                activeSignal.complete(Unit)
            } else {
                val aggregate = IllegalStateException("Module scope close failed: $owner")
                failures.forEach { aggregate.addSuppressed(it) }
                activeSignal.completeExceptionally(aggregate)
            }
        }

        if (failures.isNotEmpty()) {
            val aggregate = IllegalStateException("Module scope close failed: $owner")
            failures.forEach { aggregate.addSuppressed(it) }
            throw aggregate
        }
    }

    /**
     * Legacy interop bridge to close the module scope asynchronously from Java orchestration.
     */
    fun closeAsync(): CompletableFuture<Void?> {
        return LegacyFutureLifecycleAdapter.future(parentContext) {
            close()
        }
    }

}
