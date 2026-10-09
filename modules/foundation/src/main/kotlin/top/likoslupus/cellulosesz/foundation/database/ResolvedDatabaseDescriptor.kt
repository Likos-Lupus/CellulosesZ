package top.likoslupus.cellulosesz.foundation.database

import java.nio.file.Files
import java.nio.file.Path

/**
 * A fully resolved, ready-to-connect description of the *active* backend. It never prints its
 * secret; the password is fetched only when the pool is created.
 */
public class ResolvedDatabaseDescriptor(
    public val type: DatabaseType,
    public val namespace: String,
    public val driverClassName: String,
    public val jdbcUrl: String,
    public val username: String,
    public val secret: DatabaseSecretSpec,
    public val sslMode: String?,
    public val pool: ResolvedPoolSettings,
    public val identity: StorageIdentity,
    public val signature: StorageConnectionSignature,
    public val redactedEndpoint: String,
) {

    /** The on-disk database file for file-backed backends (SQLite/H2), or null for remote ones. */
    public val localFilePath: Path?
        get() = when (type) {
            DatabaseType.SQLITE,
            DatabaseType.H2 -> identity.path?.let(Path::of)
            DatabaseType.MYSQL,
            DatabaseType.MARIADB,
            DatabaseType.POSTGRESQL -> null
        }

    /**
     * Creates the parent directory of the local database file, if any. SQLite (and H2 file mode)
     * never creates missing directories, so a fresh world or server would otherwise fail to open
     * the database with `SQLITE_CANTOPEN`.
     */
    public fun ensureLocalStorageDirectories() {
        localFilePath?.parent?.let { Files.createDirectories(it) }
    }

    override fun toString(): String =
        "ResolvedDatabaseDescriptor(type=$type, endpoint=$redactedEndpoint)"

}

/** Turns validated [DatabaseSettings] into a concrete descriptor for the active backend. */
public object DatabaseDescriptorResolver {

    public fun resolve(
        settings: DatabaseSettings,
        storageRoot: Path
    ): ResolvedDatabaseDescriptor =
        when (settings.type) {
            DatabaseType.SQLITE -> sqlite(settings, storageRoot)
            DatabaseType.H2 -> h2(settings, storageRoot)
            DatabaseType.MYSQL -> mysql(settings)
            DatabaseType.MARIADB -> mariadb(settings)
            DatabaseType.POSTGRESQL -> postgresql(settings)
        }

    private fun sqlite(
        settings: DatabaseSettings,
        storageRoot: Path
    ): ResolvedDatabaseDescriptor {
        val sqlite = settings.sqlite
        val absolute = absolutePath(storageRoot, sqlite.path)
        val jdbcUrl = buildString {
            append("jdbc:sqlite:").append(absolute)
            append("?busy_timeout=").append(sqlite.busyTimeoutMs)
            append("&journal_mode=").append(sqlite.journalMode.uppercase())
            append("&synchronous=").append(sqlite.synchronous.uppercase())
            append("&foreign_keys=").append(sqlite.foreignKeys)
        }
        val identity = StorageIdentity(
            DatabaseType.SQLITE,
            settings.namespace,
            path = absolute
        )
        return descriptor(
            type = DatabaseType.SQLITE,
            namespace = settings.namespace,
            driverClassName = "org.sqlite.JDBC",
            jdbcUrl = jdbcUrl,
            username = "",
            secret = DatabaseSecretSpec.literal(""),
            sslMode = null,
            pool = ResolvedPoolSettings.resolve(
                DatabaseType.SQLITE,
                settings.pool,
                sqlite.pool
            ),
            identity = identity,
            redactedEndpoint = absolute,
        )
    }

    private fun h2(
        settings: DatabaseSettings,
        storageRoot: Path
    ): ResolvedDatabaseDescriptor {
        val h2 = settings.h2
        val absolute = absolutePath(storageRoot, h2.path)
        val jdbcUrl = "jdbc:h2:file:$absolute;DB_CLOSE_ON_EXIT=FALSE"
        val identity = StorageIdentity(
            DatabaseType.H2,
            settings.namespace,
            path = absolute
        )
        return descriptor(
            type = DatabaseType.H2,
            namespace = settings.namespace,
            driverClassName = "org.h2.Driver",
            jdbcUrl = jdbcUrl,
            username = h2.username,
            secret = DatabaseSecretSpec.of(h2.password, h2.passwordEnv),
            sslMode = null,
            pool = ResolvedPoolSettings.resolve(
                DatabaseType.H2,
                settings.pool,
                h2.pool
            ),
            identity = identity,
            redactedEndpoint = "h2:file:$absolute",
        )
    }

    private fun mysql(settings: DatabaseSettings): ResolvedDatabaseDescriptor {
        val mysql = settings.mysql
        val jdbcUrl = "jdbc:mysql://${mysql.host}:${mysql.port}/${mysql.database}?sslMode=${mysql.sslMode.uppercase()}"
        val identity = StorageIdentity(
            DatabaseType.MYSQL,
            settings.namespace,
            host = mysql.host,
            port = mysql.port,
            database = mysql.database,
        )
        return descriptor(
            type = DatabaseType.MYSQL,
            namespace = settings.namespace,
            driverClassName = "com.mysql.cj.jdbc.Driver",
            jdbcUrl = jdbcUrl,
            username = mysql.username,
            secret = DatabaseSecretSpec.of(mysql.password, mysql.passwordEnv),
            sslMode = mysql.sslMode,
            pool = ResolvedPoolSettings.resolve(
                DatabaseType.MYSQL,
                settings.pool,
                mysql.pool
            ),
            identity = identity,
            redactedEndpoint = "mysql://${mysql.host}:${mysql.port}/${mysql.database}",
        )
    }

    private fun mariadb(settings: DatabaseSettings): ResolvedDatabaseDescriptor {
        val mariadb = settings.mariadb
        val jdbcUrl = "jdbc:mariadb://${mariadb.host}:${mariadb.port}/${mariadb.database}?sslMode=${mariadb.sslMode.uppercase()}"
        val identity = StorageIdentity(
            DatabaseType.MARIADB,
            settings.namespace,
            host = mariadb.host,
            port = mariadb.port,
            database = mariadb.database,
        )
        return descriptor(
            type = DatabaseType.MARIADB,
            namespace = settings.namespace,
            driverClassName = "org.mariadb.jdbc.Driver",
            jdbcUrl = jdbcUrl,
            username = mariadb.username,
            secret = DatabaseSecretSpec.of(mariadb.password, mariadb.passwordEnv),
            sslMode = mariadb.sslMode,
            pool = ResolvedPoolSettings.resolve(
                DatabaseType.MARIADB,
                settings.pool,
                mariadb.pool
            ),
            identity = identity,
            redactedEndpoint = "mariadb://${mariadb.host}:${mariadb.port}/${mariadb.database}",
        )
    }

    private fun postgresql(settings: DatabaseSettings): ResolvedDatabaseDescriptor {
        val postgres = settings.postgresql
        val jdbcUrl =
            "jdbc:postgresql://${postgres.host}:${postgres.port}/${postgres.database}" +
                    "?currentSchema=${postgres.schema}&sslmode=${postgres.sslMode.lowercase()}"
        val identity = StorageIdentity(
            DatabaseType.POSTGRESQL,
            settings.namespace,
            host = postgres.host,
            port = postgres.port,
            database = postgres.database,
            schema = postgres.schema,
        )
        return descriptor(
            type = DatabaseType.POSTGRESQL,
            namespace = settings.namespace,
            driverClassName = "org.postgresql.Driver",
            jdbcUrl = jdbcUrl,
            username = postgres.username,
            secret = DatabaseSecretSpec.of(postgres.password, postgres.passwordEnv),
            sslMode = postgres.sslMode,
            pool = ResolvedPoolSettings.resolve(
                DatabaseType.POSTGRESQL,
                settings.pool,
                postgres.pool
            ),
            identity = identity,
            redactedEndpoint = "postgresql://${postgres.host}:${postgres.port}/${postgres.database}/${postgres.schema}",
        )
    }

    private fun absolutePath(storageRoot: Path, location: String): String =
        storageRoot.resolve(location).toAbsolutePath().normalize().toString()

    private fun descriptor(
        type: DatabaseType,
        namespace: String,
        driverClassName: String,
        jdbcUrl: String,
        username: String,
        secret: DatabaseSecretSpec,
        sslMode: String?,
        pool: ResolvedPoolSettings,
        identity: StorageIdentity,
        redactedEndpoint: String,
    ): ResolvedDatabaseDescriptor =
        ResolvedDatabaseDescriptor(
            type = type,
            namespace = namespace,
            driverClassName = driverClassName,
            jdbcUrl = jdbcUrl,
            username = username,
            secret = secret,
            sslMode = sslMode,
            pool = pool,
            identity = identity,
            signature = StorageConnectionSignature(
                identity = identity,
                username = username,
                secretReference = secret.reference,
                secretFingerprint = secret.fingerprint(),
                sslMode = sslMode,
                pool = pool,
            ),
            redactedEndpoint = redactedEndpoint,
        )

}
