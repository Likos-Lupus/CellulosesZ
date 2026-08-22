package top.likoslupus.cellulosesz.core.coroutine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * A coroutine-safe keyed synchronization primitive.
 *
 * Guarantees:
 * - Operations on the same key are serialized and never overlap.
 * - Operations on different keys execute concurrently.
 * - Key lock acquisition and release are structured and cancellation-safe via [Mutex.withLock].
 * - An exception thrown by an operation does not poison or leave the key locked.
 * - Lock entries are not evicted during runtime to guarantee correctness and prevent remove-races.
 * - Multi-key operations ([withLocks]) sort distinct keys deterministically using a comparator,
 *   preventing deadlocks between opposite-order concurrent callers.
 */
class KeyedMutex<K : Any> {

    private val locks = ConcurrentHashMap<K, Mutex>()

    /**
     * Returns the number of distinct keys currently tracked.
     */
    val keyCount: Int
        get() = locks.size

    /**
     * Executes the given [block] holding the lock for the specified [key].
     */
    suspend fun <T> withLock(key: K, block: suspend () -> T): T {
        val mutex = locks.computeIfAbsent(key) { Mutex() }
        return mutex.withLock { block() }
    }

    /**
     * Acquires locks for all given [keys] in deterministic order defined by [comparator]
     * and executes [block]. Releases all locks in reverse stack order upon exit.
     */
    suspend fun <T> withLocks(
        keys: Iterable<K>,
        comparator: Comparator<in K>,
        block: suspend () -> T,
    ): T {
        val distinctKeys = keys.distinct()
        return when (distinctKeys.size) {
            0 -> block()
            1 -> withLock(distinctKeys[0], block)
            else -> {
                val sortedKeys = distinctKeys.sortedWith(comparator)
                lockRecursive(sortedKeys, 0, block)
            }
        }
    }

    private suspend fun <T> lockRecursive(
        keys: List<K>,
        index: Int,
        block: suspend () -> T,
    ): T {
        return if (index >= keys.size) {
            block()
        } else {
            withLock(keys[index]) {
                lockRecursive(keys, index + 1, block)
            }
        }
    }
}

/**
 * Overload of [KeyedMutex.withLocks] for keys implementing [Comparable].
 * Uses natural ordering for deterministic lock acquisition.
 */
suspend fun <K : Comparable<K>, T> KeyedMutex<K>.withLocks(
    keys: Iterable<K>,
    block: suspend () -> T,
): T = withLocks(keys, naturalOrder(), block)
