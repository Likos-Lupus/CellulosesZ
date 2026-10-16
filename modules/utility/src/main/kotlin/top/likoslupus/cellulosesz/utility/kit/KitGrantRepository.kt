package top.likoslupus.cellulosesz.utility.kit

import java.time.Instant
import java.util.*

/**
 * Whether a system grant's items have actually been handed out yet. Persisting a [RESERVED] row
 * before delivery makes a crash fail closed (the grant is still blocked as uncertain) rather than
 * allowing a duplicate reward.
 */
internal enum class KitGrantStatus {

    RESERVED,
    DELIVERED,

}

/** A durable record of one addon reward instance for one player. */
internal data class KitGrantRecord(
    val addonId: String,
    val grantId: String,
    val playerId: UUID,
    val kitName: KitName,
    val state: KitGrantStatus,
    val createdAt: Instant,
    val deliveredAt: Instant?,
)

/**
 * The outcome of an atomic reserve attempt. [Existing] carries the already-recorded state so the
 * caller can decide between [KitGrantStatus.DELIVERED] (already processed) and
 * [KitGrantStatus.RESERVED] (previously uncertain) without a second read.
 */
internal sealed interface KitGrantReservation {

    data object Inserted : KitGrantReservation

    data class Existing(val state: KitGrantStatus) : KitGrantReservation

}

/**
 * The durable system-grant ledger in `cz_kit_grants`, keyed by
 * `(namespace, addon_id, grant_id, player_uuid)`. [reserve] writes [KitGrantStatus.RESERVED] before
 * delivery; [markDelivered] finalizes it afterwards. A reservation is never rolled back once written.
 */
internal interface KitGrantRepository {

    suspend fun find(
        addonId: String,
        grantId: String,
        playerId: UUID
    ): KitGrantRecord?

    suspend fun reserve(
        addonId: String,
        playerId: UUID,
        kitName: KitName,
        grantId: String,
        at: Instant,
    ): KitGrantReservation

    /** Finalizes a reserved grant; returns false when no matching reserved row existed. */
    suspend fun markDelivered(
        addonId: String,
        grantId: String,
        playerId: UUID,
        at: Instant,
    ): Boolean

}

/** Raised when a kit-grant row is corrupt or unsafe to load. Never silently repaired. */
internal class KitGrantDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(
    message,
    cause,
)
