package top.likoslupus.cellulosesz.core.lifecycle

/**
 * Represents a resource that supports a two-phase lifecycle teardown:
 * 1. [stopAccepting]: Reject any new incoming requests or mutations.
 * 2. [drain]: Suspend until all in-flight, already accepted operations complete.
 */
interface DrainableResource {

    /**
     * Signals this resource to stop accepting new operations immediately.
     */
    fun stopAccepting()

    /**
     * Suspends until all currently active, already accepted operations have finished.
     */
    suspend fun drain()

}
