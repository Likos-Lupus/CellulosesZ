package top.likoslupus.cellulosesz.player.presence

import java.time.Instant
import java.util.*
import kotlin.time.Duration

/** An immutable snapshot of a known player, safe to cross suspension. */
internal data class PlayerPresence(
    val id: UUID,
    val name: String,
    val firstSeen: Instant?,
    val lastSeen: Instant?,
    val online: Boolean,
    val playtime: Duration,
)

/** The outcome of resolving a player by name. Absence is distinct from being offline. */
internal sealed interface PresenceLookup {

    data class Found(val presence: PlayerPresence) : PresenceLookup

    data object NotFound : PresenceLookup

    data object StorageUnavailable : PresenceLookup

}
