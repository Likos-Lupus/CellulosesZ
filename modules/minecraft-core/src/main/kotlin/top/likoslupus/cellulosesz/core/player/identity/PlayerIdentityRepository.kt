package top.likoslupus.cellulosesz.core.player.identity

import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import java.time.Instant
import java.util.*

/** A durable, restart-surviving player identity. */
public data class PersistedPlayerIdentity(
    public val id: UUID,
    public val name: String,
    public val firstSeen: Instant,
    public val lastSeen: Instant,
)

/**
 * Durable Player Identity Index. The runtime still resolves from an in-memory cache; this repository
 * only hydrates that cache at startup and records new observations. It never performs network lookups.
 */
public interface PlayerIdentityRepository {

    public suspend fun loadAll(): List<PersistedPlayerIdentity>

    /** Records (or refreshes) a player identity. Idempotent. */
    public suspend fun observe(identity: KnownPlayerIdentity, at: Instant)

}
