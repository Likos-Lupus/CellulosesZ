package top.likoslupus.cellulosesz.foundation.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supported storage backends. Only the backend named by `database.type` is ever initialized. */
@Serializable
public enum class DatabaseType {

    @SerialName("sqlite")
    SQLITE,

    @SerialName("h2")
    H2,

    @SerialName("mysql")
    MYSQL,

    @SerialName("mariadb")
    MARIADB,

    @SerialName("postgresql")
    POSTGRESQL,

}

/** Shared Hikari timeouts applied to every backend unless a backend `[*.pool]` overrides them. */
@Serializable
public data class PoolSettings(
    @SerialName("maximum-pool-size")
    public val maximumPoolSize: Int = 10,
    @SerialName("connection-timeout-ms")
    public val connectionTimeoutMs: Long = 10_000,
    @SerialName("validation-timeout-ms")
    public val validationTimeoutMs: Long = 5_000,
    @SerialName("idle-timeout-ms")
    public val idleTimeoutMs: Long = 600_000,
    @SerialName("max-lifetime-ms")
    public val maxLifetimeMs: Long = 1_800_000,
    @SerialName("keepalive-time-ms")
    public val keepaliveTimeMs: Long = 0,
)

/** Per-backend pool overrides; a `null` field inherits the shared [PoolSettings]. */
@Serializable
public data class PoolOverride(
    @SerialName("maximum-pool-size")
    public val maximumPoolSize: Int? = null,
    @SerialName("connection-timeout-ms")
    public val connectionTimeoutMs: Long? = null,
    @SerialName("validation-timeout-ms")
    public val validationTimeoutMs: Long? = null,
    @SerialName("idle-timeout-ms")
    public val idleTimeoutMs: Long? = null,
    @SerialName("max-lifetime-ms")
    public val maxLifetimeMs: Long? = null,
    @SerialName("keepalive-time-ms")
    public val keepaliveTimeMs: Long? = null,
)

@Serializable
public data class SqliteSettings(
    @SerialName("path")
    public val path: String = "cellulosesz.db",
    @SerialName("busy-timeout-ms")
    public val busyTimeoutMs: Long = 5_000,
    @SerialName("journal-mode")
    public val journalMode: String = "wal",
    @SerialName("synchronous")
    public val synchronous: String = "normal",
    @SerialName("foreign-keys")
    public val foreignKeys: Boolean = true,
    @SerialName("pool")
    public val pool: PoolOverride = PoolOverride(maximumPoolSize = 1),
)

@Serializable
public data class H2Settings(
    @SerialName("path")
    public val path: String = "cellulosesz",
    @SerialName("username")
    public val username: String = "sa",
    @SerialName("password")
    public val password: String = "",
    @SerialName("password-env")
    public val passwordEnv: String = "",
    @SerialName("pool")
    public val pool: PoolOverride = PoolOverride(maximumPoolSize = 2),
)

@Serializable
public data class MySqlSettings(
    @SerialName("host")
    public val host: String = "127.0.0.1",
    @SerialName("port")
    public val port: Int = 3306,
    @SerialName("database")
    public val database: String = "cellulosesz",
    @SerialName("username")
    public val username: String = "cellulosesz",
    @SerialName("password")
    public val password: String = "",
    @SerialName("password-env")
    public val passwordEnv: String = "",
    @SerialName("ssl-mode")
    public val sslMode: String = "preferred",
    @SerialName("pool")
    public val pool: PoolOverride = PoolOverride(maximumPoolSize = 10),
)

@Serializable
public data class MariaDbSettings(
    @SerialName("host")
    public val host: String = "127.0.0.1",
    @SerialName("port")
    public val port: Int = 3306,
    @SerialName("database")
    public val database: String = "cellulosesz",
    @SerialName("username")
    public val username: String = "cellulosesz",
    @SerialName("password")
    public val password: String = "",
    @SerialName("password-env")
    public val passwordEnv: String = "",
    @SerialName("ssl-mode")
    public val sslMode: String = "preferred",
    @SerialName("pool")
    public val pool: PoolOverride = PoolOverride(maximumPoolSize = 10),
)

@Serializable
public data class PostgreSqlSettings(
    @SerialName("host")
    public val host: String = "127.0.0.1",
    @SerialName("port")
    public val port: Int = 5432,
    @SerialName("database")
    public val database: String = "cellulosesz",
    @SerialName("schema")
    public val schema: String = "public",
    @SerialName("username")
    public val username: String = "cellulosesz",
    @SerialName("password")
    public val password: String = "",
    @SerialName("password-env")
    public val passwordEnv: String = "",
    @SerialName("ssl-mode")
    public val sslMode: String = "prefer",
    @SerialName("pool")
    public val pool: PoolOverride = PoolOverride(maximumPoolSize = 10),
)

/** Root of the `[database]` section. Each backend owns an independent section. */
@Serializable
public data class DatabaseSettings(
    @SerialName("type")
    public val type: DatabaseType = DatabaseType.SQLITE,
    @SerialName("namespace")
    public val namespace: String = "default",
    @SerialName("pool")
    public val pool: PoolSettings = PoolSettings(),
    @SerialName("sqlite")
    public val sqlite: SqliteSettings = SqliteSettings(),
    @SerialName("h2")
    public val h2: H2Settings = H2Settings(),
    @SerialName("mysql")
    public val mysql: MySqlSettings = MySqlSettings(),
    @SerialName("mariadb")
    public val mariadb: MariaDbSettings = MariaDbSettings(),
    @SerialName("postgresql")
    public val postgresql: PostgreSqlSettings = PostgreSqlSettings(),
)
