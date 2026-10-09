package top.likoslupus.cellulosesz.foundation.database

import kotlinx.serialization.Serializable

/** Effective pool settings: shared values merged with the active backend's overrides. */
@Serializable
public data class ResolvedPoolSettings(
    public val maximumPoolSize: Int,
    public val connectionTimeoutMs: Long,
    public val validationTimeoutMs: Long,
    public val idleTimeoutMs: Long,
    public val maxLifetimeMs: Long,
    public val keepaliveTimeMs: Long,
) {

    public companion object {

        /** SQLite is serialized through a single connection; other backends default to the shared size. */
        public fun resolve(
            type: DatabaseType,
            common: PoolSettings,
            override: PoolOverride,
        ): ResolvedPoolSettings =
            ResolvedPoolSettings(
                maximumPoolSize = override.maximumPoolSize
                    ?: when (type) {
                        DatabaseType.SQLITE -> 1
                        DatabaseType.H2 -> 2
                        else -> common.maximumPoolSize
                    },
                connectionTimeoutMs = override.connectionTimeoutMs
                    ?: common.connectionTimeoutMs,
                validationTimeoutMs = override.validationTimeoutMs
                    ?: common.validationTimeoutMs,
                idleTimeoutMs = override.idleTimeoutMs
                    ?: common.idleTimeoutMs,
                maxLifetimeMs = override.maxLifetimeMs
                    ?: common.maxLifetimeMs,
                keepaliveTimeMs = override.keepaliveTimeMs
                    ?: common.keepaliveTimeMs,
            )

    }

}
