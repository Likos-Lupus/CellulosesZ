package top.likoslupus.cellulosesz.foundation.persistence

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-key serialization for concurrent file access. Entries are retained for the lifetime of the
 * store; keys are bounded by the player/resource count.
 */
public class KeyedMutex<K : Any> {

    private val locks = ConcurrentHashMap<K, Mutex>()

    private fun lockFor(key: K): Mutex =
        locks.computeIfAbsent(key) { Mutex() }

    public suspend fun <T> withLock(
        key: K,
        action: suspend () -> T
    ): T =
        lockFor(key).withLock { action() }

}
