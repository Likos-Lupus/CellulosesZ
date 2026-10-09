package top.likoslupus.cellulosesz.foundation.database.state

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.foundation.database.*

/**
 * A machine-state record of the storage endpoint that last booted successfully. It lets a changed
 * `[database]` section still find the *previous* endpoint to migrate from, and lets a completed
 * migration finalize after a crash between the target commit and this sidecar being rewritten.
 *
 * It holds a secret reference (never printed) to reconnect the old endpoint.
 */
@Serializable
public data class LastStorageState(
    public val type: DatabaseType,
    public val namespace: String,
    public val host: String? = null,
    public val port: Int? = null,
    public val database: String? = null,
    public val schema: String? = null,
    public val path: String? = null,
    public val driverClassName: String,
    public val jdbcUrl: String,
    public val username: String,
    public val secret: DatabaseSecretSpec,
    public val sslMode: String? = null,
    public val pool: ResolvedPoolSettings,
    public val recordedAtEpochMs: Long,
) {

    public val identity: StorageIdentity
        get() = StorageIdentity(type, namespace, host, port, database, schema, path)

    public fun toDescriptor(): ResolvedDatabaseDescriptor =
        identity.let {
            ResolvedDatabaseDescriptor(
                type = type,
                namespace = namespace,
                driverClassName = driverClassName,
                jdbcUrl = jdbcUrl,
                username = username,
                secret = secret,
                sslMode = sslMode,
                pool = pool,
                identity = it,
                signature = StorageConnectionSignature(
                    identity = it,
                    username = username,
                    secretReference = secret.reference,
                    secretFingerprint = secret.fingerprint(),
                    sslMode = sslMode,
                    pool = pool,
                ),
                redactedEndpoint = redactedEndpoint(),
            )
        }

    private fun redactedEndpoint(): String =
        when (type) {
            DatabaseType.SQLITE, DatabaseType.H2 -> path.orEmpty()
            DatabaseType.MYSQL -> "mysql://$host:$port/$database"
            DatabaseType.MARIADB -> "mariadb://$host:$port/$database"
            DatabaseType.POSTGRESQL -> "postgresql://$host:$port/$database/$schema"
        }

    public companion object {

        public fun from(
            descriptor: ResolvedDatabaseDescriptor,
            nowEpochMs: Long
        ): LastStorageState =
            descriptor.identity.let {
                LastStorageState(
                    type = it.type,
                    namespace = it.namespace,
                    host = it.host,
                    port = it.port,
                    database = it.database,
                    schema = it.schema,
                    path = it.path,
                    driverClassName = descriptor.driverClassName,
                    jdbcUrl = descriptor.jdbcUrl,
                    username = descriptor.username,
                    secret = descriptor.secret,
                    sslMode = descriptor.sslMode,
                    pool = descriptor.pool,
                    recordedAtEpochMs = nowEpochMs,
                )
            }

    }

}
