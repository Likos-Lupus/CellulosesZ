package top.likoslupus.cellulosesz.core.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Explicit container for core coroutine dispatchers.
 *
 * Dispatcher Ownership:
 * - [application]: Used for loader-neutral background application work and general coroutine execution.
 *   Defaults to [Dispatchers.Default].
 * - [storage]: Dedicated to disk I/O and document serialization. Defaults to [Dispatchers.IO].
 *
 * Server Boundary & Borrowed Resources:
 * - Standard dispatchers ([Dispatchers.Default], [Dispatchers.IO]) are borrowed from the JVM/runtime and are never closed.
 * - The concrete Minecraft server thread coroutine dispatcher is deferred to Phase 6 in `cellulosesz-common`.
 *   The core runtime treats platform/server dispatchers as borrowed execution contexts.
 */
data class RuntimeDispatchers(
    val application: CoroutineDispatcher = Dispatchers.Default,
    val storage: CoroutineDispatcher = Dispatchers.IO,
)
