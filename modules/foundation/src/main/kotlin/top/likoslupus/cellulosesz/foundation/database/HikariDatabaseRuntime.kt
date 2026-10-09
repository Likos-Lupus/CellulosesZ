package top.likoslupus.cellulosesz.foundation.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

/** The only place HikariCP types exist. Business code sees [DatabaseRuntime] + `java.sql` only. */
public class HikariDatabaseRuntime private constructor(
    private val dataSource: HikariDataSource,
    override val type: DatabaseType,
    override val identity: StorageIdentity,
) : DatabaseRuntime {

    override fun <T> read(block: (Connection) -> T): T =
        dataSource.connection.use(block)

    override fun <T> transaction(block: (Connection) -> T): T =
        dataSource.connection.use { connection ->
            val previous = connection.autoCommit
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                result
            } catch (exception: Throwable) {
                runCatching { connection.rollback() }
                throw exception
            } finally {
                runCatching { connection.autoCommit = previous }
            }
        }

    override fun close() {
        dataSource.close()
    }

    public companion object {

        /** Creates and validates the pool. Fails fast: an unreachable backend throws. */
        public fun create(descriptor: ResolvedDatabaseDescriptor): HikariDatabaseRuntime {
            val pool = descriptor.pool
            val config = HikariConfig().apply {
                poolName = "cellulosesz-${descriptor.type.name.lowercase()}"
                jdbcUrl = descriptor.jdbcUrl
                driverClassName = descriptor.driverClassName
                if (descriptor.username.isNotEmpty()) {
                    username = descriptor.username
                }
                password = descriptor.secret.resolve()
                maximumPoolSize = pool.maximumPoolSize
                connectionTimeout = pool.connectionTimeoutMs
                validationTimeout = pool.validationTimeoutMs
                idleTimeout = pool.idleTimeoutMs
                maxLifetime = pool.maxLifetimeMs
                if (pool.keepaliveTimeMs >= MIN_KEEPALIVE_MS) {
                    keepaliveTime = pool.keepaliveTimeMs
                }
                isAutoCommit = true
                connectionTestQuery = /* language=SQL */ "SELECT 1"
            }
            val dataSource = HikariDataSource(config)
            return HikariDatabaseRuntime(dataSource, descriptor.type, descriptor.identity)
        }

        private const val MIN_KEEPALIVE_MS = 30_000L

    }

}
