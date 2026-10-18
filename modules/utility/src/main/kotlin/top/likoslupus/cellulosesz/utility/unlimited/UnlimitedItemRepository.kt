package top.likoslupus.cellulosesz.utility.unlimited

import java.util.*

/** The per-player unlimited item set stored in `cz_unlimited_items`. */
internal interface UnlimitedItemRepository {

    suspend fun loadAll(): Map<UUID, Set<String>>

    /** Returns true when the item was newly added. */
    suspend fun add(playerId: UUID, itemId: String): Boolean

    /** Returns true when the item was removed. */
    suspend fun remove(playerId: UUID, itemId: String): Boolean

    /** Returns the number of rows removed. */
    suspend fun clear(playerId: UUID): Int

}

/** Raised when an unlimited-items row cannot be read or written safely. */
internal class UnlimitedItemDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
