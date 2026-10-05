package top.likoslupus.cellulosesz.utility.kit

import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import java.time.Instant
import java.util.*

/**
 * Per-player durable kit-use history. Claims live in separate per-player files so a single corrupt
 * file only degrades that player, never the whole catalog.
 */
internal interface KitClaimRepository {

    suspend fun load(playerId: UUID): Map<KitName, KitClaim>

    /** Durably records intent to deliver before the items are handed out. */
    suspend fun reserve(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant,
    )

    /** Durably finalizes a claim once delivery has happened. */
    suspend fun markDelivered(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant,
    )

    /** Removes a single kit's claim; returns false when there was nothing to reset. */
    suspend fun reset(
        playerId: UUID,
        name: KitName,
    ): Boolean

}

/** Raised when a claim file is corrupt or unsafe to load. Never silently overwritten. */
internal class KitClaimDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(
    message,
    cause,
)
