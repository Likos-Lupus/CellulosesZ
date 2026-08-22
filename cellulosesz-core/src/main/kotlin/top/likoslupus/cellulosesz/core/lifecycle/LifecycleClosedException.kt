package top.likoslupus.cellulosesz.core.lifecycle

/**
 * Thrown when an operation or resource registration is submitted to a lifecycle scope or gate
 * that is stopping or has already been stopped.
 */
class LifecycleClosedException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
