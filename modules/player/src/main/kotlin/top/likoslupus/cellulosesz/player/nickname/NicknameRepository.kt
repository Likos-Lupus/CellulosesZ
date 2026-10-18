package top.likoslupus.cellulosesz.player.nickname

import java.time.Instant
import java.util.*

/**
 * One durable nickname row. It never carries a live player reference. [updatedBy] records the staff
 * member who set it through `/nick setfor`, or null when the owner set it themselves.
 *
 * [ownerName] is the account name at the time the nickname was set; `/nick` uses it to discard a
 * nickname when `resetOnNameChange` is enabled and the owner has since changed their account name.
 */
internal data class StoredNickname(
    val playerId: UUID,
    val display: String,
    val normalized: String,
    val updatedAt: Instant,
    val updatedBy: UUID? = null,
    val ownerName: String? = null,
)

/**
 * Durable nicknames in `cz_player_nicknames`. It owns all nickname SQL; callers stay on immutable
 * values. Uniqueness is enforced by the normalized column and its unique index, not by the caller.
 */
internal interface NicknameRepository {

    /** The nickname owned by one player, or null when they have none. */
    suspend fun find(playerId: UUID): StoredNickname?

    /** The nickname carrying one normalized form, or null when it is free. */
    suspend fun findByNormalized(normalized: String): StoredNickname?

    /** Inserts or replaces the nickname owned by [record.playerId]. */
    suspend fun save(record: StoredNickname)

    /** Removes one player's nickname; returns false when they had none. */
    suspend fun delete(playerId: UUID): Boolean

}
