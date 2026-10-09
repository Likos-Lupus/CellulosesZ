package top.likoslupus.cellulosesz.foundation.database

import kotlinx.serialization.Serializable

/**
 * Identifies how a connection is established to a fixed [identity]. A change here means "reconnect
 * with the same data", not "move the data". Used to reject runtime reloads that would need a pool
 * swap.
 */
@Serializable
public data class StorageConnectionSignature(
    public val identity: StorageIdentity,
    public val username: String,
    public val secretReference: String,
    public val secretFingerprint: String,
    public val sslMode: String? = null,
    public val pool: ResolvedPoolSettings,
)
