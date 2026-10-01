package top.likoslupus.cellulosesz.persistence

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class KeyedMutex<K : Any> {

    private val locks = ConcurrentHashMap<K, Mutex>()

    private fun lockFor(key: K): Mutex =
        locks.computeIfAbsent(key) { Mutex() }

    suspend fun <T> withLock(key: K, action: suspend () -> T): T =
        lockFor(key).withLock { action() }

}
