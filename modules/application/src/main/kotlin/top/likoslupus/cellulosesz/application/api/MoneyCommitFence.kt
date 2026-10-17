package top.likoslupus.cellulosesz.application.api

import java.util.concurrent.atomic.AtomicReference

/** The single-shot lifecycle of a money request around its commit boundary. */
internal enum class MoneyRequestPhase {

    PREPARING,
    COMMITTING,
    FINISHED,
    CANCELLED,

}

/**
 * A conservative commit fence for a request that moves money. A request may only be cancelled while
 * it is still [MoneyRequestPhase.PREPARING]; once [tryBeginCommit] has claimed the commit region a
 * blocking JDBC transaction may already be running and cannot be interrupted, so cancellation is
 * refused. This makes `cancel() == true` a guarantee that no funds change was started, while
 * `cancel() == false` means the caller must recover the outcome by its request key.
 *
 * [finish] is a no-op for an already-cancelled request: it never overwrites a cancellation.
 */
internal class MoneyCommitFence {

    private val phase = AtomicReference(MoneyRequestPhase.PREPARING)

    /** Claims the commit region; returns false if the request was already cancelled. */
    fun tryBeginCommit(): Boolean =
        phase.compareAndSet(
            MoneyRequestPhase.PREPARING,
            MoneyRequestPhase.COMMITTING
        )

    /** Cancels only before the commit region begins. */
    fun cancelBeforeCommit(): Boolean =
        phase.compareAndSet(
            MoneyRequestPhase.PREPARING,
            MoneyRequestPhase.CANCELLED
        )

    /** Marks the request complete without clobbering a cancellation. */
    fun finish() {
        phase.getAndUpdate { current ->
            when (current) {
                MoneyRequestPhase.COMMITTING -> MoneyRequestPhase.FINISHED
                else -> current
            }
        }
    }

    fun isCancelled(): Boolean =
        phase.get() == MoneyRequestPhase.CANCELLED

}
