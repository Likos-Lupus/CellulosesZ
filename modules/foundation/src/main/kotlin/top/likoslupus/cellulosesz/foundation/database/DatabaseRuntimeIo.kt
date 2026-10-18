package top.likoslupus.cellulosesz.foundation.database

import java.sql.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a blocking [DatabaseRuntime.read] on the IO dispatcher. This is the only supported way for a
 * suspend repository to enter JDBC: it keeps every call off the server thread and removes the repeated
 * `withContext(Dispatchers.IO)` wrapper. The transaction variant is the exact counterpart.
 */
public suspend fun <T> DatabaseRuntime.readIo(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        read(block)
    }

/** Runs a blocking [DatabaseRuntime.transaction] on the IO dispatcher. */
public suspend fun <T> DatabaseRuntime.transactionIo(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        transaction(block)
    }
