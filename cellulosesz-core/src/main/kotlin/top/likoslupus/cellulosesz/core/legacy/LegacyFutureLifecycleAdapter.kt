package top.likoslupus.cellulosesz.core.legacy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Internal legacy lifecycle adapter isolating [CompletableFuture] and [AsyncCloseable] bridges.
 *
 * This adapter is temporary and scheduled for deletion when unmigrated Java services
 * and orchestration are rewritten in their respective roadmap phases.
 */
object LegacyFutureLifecycleAdapter {

    /**
     * Bridges a suspending block to a [CompletableFuture] without blocking threads.
     */
    fun future(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): CompletableFuture<Void?> = CoroutineScope(context).future {
        block()
        null
    }

    /**
     * Bridges a suspending block to a [CompletableFuture] returning Java [Void].
     */
    @Suppress("UNCHECKED_CAST")
    fun futureVoid(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): CompletableFuture<Void> = CoroutineScope(context).future {
        block()
        null
    } as CompletableFuture<Void>

}
