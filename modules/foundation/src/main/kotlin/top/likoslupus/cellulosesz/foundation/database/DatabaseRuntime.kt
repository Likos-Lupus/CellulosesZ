package top.likoslupus.cellulosesz.foundation.database

import java.sql.Connection

/**
 * Blocking JDBC access with a bounded lifetime. A [Connection] is never held across suspension;
 * callers run this inside `withContext(ioDispatcher)`. `transaction` sets and resets autocommit.
 */
public interface DatabaseRuntime : AutoCloseable {

    public val type: DatabaseType

    public val identity: StorageIdentity

    public fun <T> read(block: (Connection) -> T): T

    public fun <T> transaction(block: (Connection) -> T): T

}
