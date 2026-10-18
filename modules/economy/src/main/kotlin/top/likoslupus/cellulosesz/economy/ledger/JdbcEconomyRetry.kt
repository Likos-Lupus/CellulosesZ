package top.likoslupus.cellulosesz.economy.ledger

import java.sql.SQLException

/** How many times a whole transaction is attempted before a transient failure is surfaced. */
internal const val MAX_COMMIT_ATTEMPTS: Int = 3

/** Base backoff between transient retries, multiplied by the attempt number and applied off the JDBC block. */
internal const val BASE_BACKOFF_MS: Long = 25

/**
 * Recognizes the well-known *transient* backend failures (deadlock, serialization conflict, lock
 * wait timeout) that a retry of the whole transaction can resolve. Anything unrecognized is treated
 * as fatal so a genuine data or permission error is never retried.
 */
internal fun isTransientBackendFailure(exception: SQLException): Boolean {
    val state = exception.sqlState
    return when {
        // transaction rollback: serialization failure / deadlock (PostgreSQL, H2)
        state != null && state.startsWith("40") -> true
        else -> when (exception.errorCode) {
            // MySQL / MariaDB: deadlock, lock wait timeout
            1213, 1205 -> true
            // H2: lock timeout
            50200 -> true
            // SQLite: BUSY / LOCKED (including extended codes)
            else -> exception.errorCode and 0xFF in 5..6
        }
    }
}
