package top.likoslupus.cellulosesz.utility.unlimited

import java.util.*

/** Server-thread-confined inventory access for the unlimited item feature. */
internal interface UnlimitedBackend {

    /** The registry id of the player's held item, or null when offline or holding nothing. */
    fun heldItemId(playerId: UUID): String?

    /** Applies the top-up plan to the player's inventory; returns the number of slots changed. */
    fun topUp(playerId: UUID, unlimited: Set<String>): Int

}
