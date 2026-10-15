package top.likoslupus.cellulosesz.api

import java.util.*

/**
 * A read-only player identity. [lastKnownName] is the last name CellulosesZ saw; it is not a live
 * Mojang lookup and must not be treated as the player's current account name.
 */
public class PlayerIdentity(
    public val uuid: UUID,
    public val lastKnownName: String,
)

/**
 * Read-only identity lookup. Implementations are read-only and expose no storage, IP or moderation
 * data. Results are valid only while the API is ready.
 */
public fun interface PlayerLookup {

    /** `Available(null)` means the UUID is not known; `NotReady`/`Stopping` are different states. */
    public fun findById(id: UUID): ApiQuery<PlayerIdentity?>

}
