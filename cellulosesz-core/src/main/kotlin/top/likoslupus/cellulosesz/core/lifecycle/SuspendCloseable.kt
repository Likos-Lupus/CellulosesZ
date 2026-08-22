package top.likoslupus.cellulosesz.core.lifecycle

/**
 * Represents a resource that requires suspending asynchronous teardown.
 */
fun interface SuspendCloseable {

    /**
     * Closes this resource asynchronously.
     */
    suspend fun close()

}
