package top.likoslupus.cellulosesz.utility.powertool

import java.time.Instant
import java.util.*

/** One item → command binding for a player, keyed by the item registry id and the trigger. */
internal data class PowertoolBinding(
    val itemId: String,
    val trigger: PowertoolTrigger,
    val command: String,
)

/** Persistence for `cz_powertools`. */
internal interface PowertoolRepository {

    suspend fun loadAll(): Map<UUID, List<PowertoolBinding>>

    /** Upserts one binding; returns true when the row was written. */
    suspend fun bind(
        playerId: UUID,
        binding: PowertoolBinding,
        at: Instant,
    ): Boolean

    /** Removes every binding for one item; returns the number of rows removed. */
    suspend fun removeAll(playerId: UUID, itemId: String): Int

}

/** Raised when a powertool row cannot be read or written safely. */
internal class PowertoolDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
