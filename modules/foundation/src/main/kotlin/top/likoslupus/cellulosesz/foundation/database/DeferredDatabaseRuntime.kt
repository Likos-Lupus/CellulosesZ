package top.likoslupus.cellulosesz.foundation.database

import java.sql.Connection
import java.util.concurrent.atomic.AtomicReference

/**
 * Placeholder injected into feature repositories at mod-init. The real runtime is bound during the
 * blocking storage bootstrap at `SERVER_STARTING`, before any repository call can happen.
 */
public class DeferredDatabaseRuntime : DatabaseRuntime {

    private val delegateRef = AtomicReference<DatabaseRuntime?>(null)

    public val initialized: Boolean
        get() = delegateRef.get() != null

    public fun initialize(runtime: DatabaseRuntime) {
        check(
            delegateRef.compareAndSet(
                null,
                runtime
            )
        ) { "database runtime already initialized" }
    }

    private fun delegate(): DatabaseRuntime =
        delegateRef.get()
            ?: error("database runtime is not initialized")

    override val type: DatabaseType
        get() = delegate().type

    override val identity: StorageIdentity
        get() = delegate().identity

    override fun <T> read(block: (Connection) -> T): T =
        delegate().read(block)

    override fun <T> transaction(block: (Connection) -> T): T =
        delegate().transaction(block)

    override fun close() {
        delegateRef.getAndSet(null)?.close()
    }

}
