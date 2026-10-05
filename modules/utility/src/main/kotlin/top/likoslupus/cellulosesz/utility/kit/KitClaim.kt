package top.likoslupus.cellulosesz.utility.kit

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

/**
 * Whether a kit claim's items have actually been handed out yet. Persisting a [RESERVED] record
 * before delivery makes a server crash fail closed (the player is still blocked) rather than
 * allowing a duplicate delivery.
 */
@Serializable
internal enum class KitClaimStatus {

    RESERVED,
    DELIVERED,

}

/** A durable record of one player's use of one kit definition. */
internal data class KitClaim(
    val kitId: UUID,
    val claimedAt: Instant,
    val status: KitClaimStatus,
)
