package top.likoslupus.cellulosesz.foundation.persistence

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KeyedMutexTest {

    @Test
    fun `serializes concurrent access for the same key`() =
        runBlocking {
            val mutex = KeyedMutex<String>()
            var counter = 0

            coroutineScope {
                repeat(100) {
                    launch {
                        mutex.withLock("same") {
                            val current = counter
                            counter = current + 1
                        }
                    }
                }
            }

            assertEquals(100, counter)
        }

    @Test
    fun `allows independent keys`() =
        runBlocking {
            val mutex = KeyedMutex<String>()
            var counter = 0

            coroutineScope {
                repeat(50) {
                    launch {
                        mutex.withLock("a") {
                            val current = counter
                            counter = current + 1
                        }
                    }
                    launch {
                        mutex.withLock("b") {
                            val current = counter
                            counter = current + 1
                        }
                    }
                }
            }

            assertEquals(100, counter)
        }

}
