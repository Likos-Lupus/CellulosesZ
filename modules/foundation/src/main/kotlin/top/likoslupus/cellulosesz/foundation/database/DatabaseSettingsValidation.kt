package top.likoslupus.cellulosesz.foundation.database

import top.likoslupus.cellulosesz.foundation.config.ValidationError

/** Structural validation for the whole `[database]` tree, independent of the active backend. */
public object DatabaseSettingsValidation {

    private val namespacePattern = Regex("[a-z0-9_]{1,32}")

    private val sqliteJournalModes = setOf(
        "delete",
        "truncate",
        "persist",
        "memory",
        "wal",
        "off"
    )
    private val sqliteSynchronous = setOf(
        "off",
        "normal",
        "full",
        "extra"
    )
    private val mysqlSslModes = setOf(
        "disabled",
        "preferred",
        "required",
        "verify_ca",
        "verify_identity"
    )

    public fun validate(settings: DatabaseSettings): List<ValidationError> =
        buildList {
            if (!namespacePattern.matches(settings.namespace)) {
                add(
                    ValidationError(
                        "database.namespace",
                        "namespace must match [a-z0-9_]{1,32}",
                    )
                )
            }
            addPool(
                "database.pool",
                settings.pool
            )

            if (settings.sqlite.path.isBlank()) {
                add(
                    ValidationError(
                        "database.sqlite.path",
                        "path must not be blank"
                    )
                )
            }
            if (settings.sqlite.busyTimeoutMs < 0) {
                add(
                    ValidationError(
                        "database.sqlite.busy-timeout-ms",
                        "must be >= 0"
                    )
                )
            }
            if (settings.sqlite.journalMode.lowercase() !in sqliteJournalModes) {
                add(
                    ValidationError(
                        "database.sqlite.journal-mode",
                        "unsupported journal mode"
                    )
                )
            }
            if (settings.sqlite.synchronous.lowercase() !in sqliteSynchronous) {
                add(
                    ValidationError(
                        "database.sqlite.synchronous",
                        "unsupported synchronous mode"
                    )
                )
            }
            addPool(
                "database.sqlite.pool",
                settings.sqlite.pool
            )

            if (settings.h2.path.isBlank()) {
                add(
                    ValidationError(
                        "database.h2.path",
                        "path must not be blank"
                    )
                )
            }
            if (settings.h2.username.isBlank()) {
                add(
                    ValidationError(
                        "database.h2.username",
                        "username must not be blank"
                    )
                )
            }
            addSecret(
                "database.h2",
                settings.h2.password,
                settings.h2.passwordEnv
            )
            addPool(
                "database.h2.pool",
                settings.h2.pool
            )

            validateRemote(
                path = "database.mysql",
                host = settings.mysql.host,
                port = settings.mysql.port,
                database = settings.mysql.database,
                username = settings.mysql.username,
                password = settings.mysql.password,
                passwordEnv = settings.mysql.passwordEnv,
                sslMode = settings.mysql.sslMode,
                sslModes = mysqlSslModes,
            )
            addPool(
                "database.mysql.pool",
                settings.mysql.pool
            )

            validateRemote(
                path = "database.mariadb",
                host = settings.mariadb.host,
                port = settings.mariadb.port,
                database = settings.mariadb.database,
                username = settings.mariadb.username,
                password = settings.mariadb.password,
                passwordEnv = settings.mariadb.passwordEnv,
                sslMode = settings.mariadb.sslMode,
                sslModes = mysqlSslModes,
            )
            addPool(
                "database.mariadb.pool",
                settings.mariadb.pool
            )

            validateRemote(
                path = "database.postgresql",
                host = settings.postgresql.host,
                port = settings.postgresql.port,
                database = settings.postgresql.database,
                username = settings.postgresql.username,
                password = settings.postgresql.password,
                passwordEnv = settings.postgresql.passwordEnv,
                sslMode = settings.postgresql.sslMode,
                sslModes = null,
            )
            if (settings.postgresql.schema.isBlank()) {
                add(
                    ValidationError(
                        "database.postgresql.schema",
                        "schema must not be blank"
                    )
                )
            }
            addPool(
                "database.postgresql.pool",
                settings.postgresql.pool
            )
        }

    private fun MutableList<ValidationError>.addPool(path: String, pool: PoolSettings) {
        if (pool.maximumPoolSize !in 1..1000) {
            add(
                ValidationError(
                    "$path.maximum-pool-size",
                    "must be in 1..1000"
                )
            )
        }
        if (pool.connectionTimeoutMs < 0) {
            add(
                ValidationError(
                    "$path.connection-timeout-ms",
                    "must be >= 0"
                )
            )
        }
        if (pool.validationTimeoutMs < 0) {
            add(
                ValidationError(
                    "$path.validation-timeout-ms",
                    "must be >= 0"
                )
            )
        }
        if (pool.idleTimeoutMs < 0) {
            add(
                ValidationError(
                    "$path.idle-timeout-ms",
                    "must be >= 0"
                )
            )
        }
        if (pool.maxLifetimeMs < 0) {
            add(
                ValidationError(
                    "$path.max-lifetime-ms",
                    "must be >= 0"
                )
            )
        }
        if (pool.keepaliveTimeMs < 0) {
            add(
                ValidationError(
                    "$path.keepalive-time-ms",
                    "must be >= 0"
                )
            )
        }
    }

    private fun MutableList<ValidationError>.addPool(path: String, pool: PoolOverride) {
        pool.maximumPoolSize?.let {
            if (it !in 1..1000) {
                add(
                    ValidationError(
                        "$path.maximum-pool-size",
                        "must be in 1..1000"
                    )
                )
            }
        }
        pool.connectionTimeoutMs?.let {
            if (it < 0) {
                add(
                    ValidationError(
                        "$path.connection-timeout-ms",
                        "must be >= 0"
                    )
                )
            }
        }
        pool.validationTimeoutMs?.let {
            if (it < 0) {
                add(
                    ValidationError(
                        "$path.validation-timeout-ms",
                        "must be >= 0"
                    )
                )
            }
        }
        pool.idleTimeoutMs?.let {
            if (it < 0) {
                add(
                    ValidationError(
                        "$path.idle-timeout-ms",
                        "must be >= 0"
                    )
                )
            }
        }
        pool.maxLifetimeMs?.let {
            if (it < 0) {
                add(
                    ValidationError(
                        "$path.max-lifetime-ms",
                        "must be >= 0"
                    )
                )
            }
        }
        pool.keepaliveTimeMs?.let {
            if (it < 0) {
                add(
                    ValidationError(
                        "$path.keepalive-time-ms",
                        "must be >= 0"
                    )
                )
            }
        }
    }

    private fun MutableList<ValidationError>.addSecret(
        path: String,
        password: String,
        passwordEnv: String
    ) {
        if (password.isNotEmpty() && passwordEnv.isNotEmpty()) {
            add(
                ValidationError(
                    path,
                    "set at most one of password/password-env"
                )
            )
        }
    }

    private fun MutableList<ValidationError>.validateRemote(
        path: String,
        host: String,
        port: Int,
        database: String,
        username: String,
        password: String,
        passwordEnv: String,
        sslMode: String,
        sslModes: Set<String>?,
    ) {
        if (host.isBlank()) {
            add(
                ValidationError(
                    "$path.host",
                    "host must not be blank"
                )
            )
        }
        if (port !in 1..65535) {
            add(
                ValidationError(
                    "$path.port",
                    "port must be in 1..65535"
                )
            )
        }
        if (database.isBlank()) {
            add(
                ValidationError(
                    "$path.database",
                    "database must not be blank"
                )
            )
        }
        if (username.isBlank()) {
            add(
                ValidationError(
                    "$path.username",
                    "username must not be blank"
                )
            )
        }
        addSecret(path, password, passwordEnv)
        if (sslModes != null
            && sslMode.lowercase() !in sslModes
        ) {
            add(
                ValidationError(
                    "$path.ssl-mode",
                    "unsupported ssl mode"
                )
            )
        }
    }

}
