package top.likoslupus.cellulosesz.api.kit

import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Duration
import java.util.*

/** The reuse policy of a kit, as a stable public projection. */
public sealed interface KitReuseView {

    public data object Always : KitReuseView

    public data object Once : KitReuseView

    public data class Cooldown(
        public val duration: Duration
    ) : KitReuseView

}

/** Whether a specific player may claim now. */
public sealed interface KitAvailabilityView {

    public data object Available : KitAvailabilityView

    public data object AlreadyUsed : KitAvailabilityView

    public data class Cooldown(
        public val remaining: Duration
    ) : KitAvailabilityView

}

public class KitSummary(
    public val name: String,
    public val reuse: KitReuseView,
    public val availability: KitAvailabilityView,
)

public sealed interface KitListOutcome {

    public data class Listed(public val kits: List<KitSummary>) : KitListOutcome

    public data object Disabled : KitListOutcome

    public data object Loading : KitListOutcome

    public data object StorageUnavailable : KitListOutcome

    public data object NotAuthorized : KitListOutcome

}

public sealed interface KitAvailabilityOutcome {

    public data class Found(
        public val availability: KitAvailabilityView
    ) : KitAvailabilityOutcome

    public data object NotFound : KitAvailabilityOutcome

    public data object Disabled : KitAvailabilityOutcome

    public data object Loading : KitAvailabilityOutcome

    public data object StorageUnavailable : KitAvailabilityOutcome

    public data object NotAuthorized : KitAvailabilityOutcome

}

public sealed interface KitClaimOutcome {

    public data class Delivered(
        public val droppedStacks: Int,
        public val claimRecordedFully: Boolean,
        public val equippedStacks: Int = 0,
    ) : KitClaimOutcome

    public data object Disabled : KitClaimOutcome

    public data object Loading : KitClaimOutcome

    public data object NotFound : KitClaimOutcome

    public data object InvalidName : KitClaimOutcome

    public data class Cooldown(
        public val remaining: Duration
    ) : KitClaimOutcome

    public data object AlreadyUsed : KitClaimOutcome

    public data object InventoryFull : KitClaimOutcome

    public data object PlayerOffline : KitClaimOutcome

    public data object NotAuthorized : KitClaimOutcome

    public data object StorageUnavailable : KitClaimOutcome

    /** Items were reserved but final delivery could not be confirmed — never auto-retried. */
    public data object DeliveryFailedAfterReservation : KitClaimOutcome

    /** An addon eligibility check denied the claim before any precheck or reservation. */
    public data class Denied(
        public val reasonKey: String
    ) : KitClaimOutcome

}

/**
 * A system reward request. [grantId] is a stable, reward-instance identity provided by the addon; it
 * is the durable de-duplication key.
 */
public class AddonKitGrantRequest(
    public val access: AddonAccess,
    public val playerId: UUID,
    public val kitName: String,
    public val grantId: String,
)

public sealed interface KitGrantOutcome {

    public data class Delivered(
        public val grantId: String,
        public val droppedStacks: Int,
        public val equippedStacks: Int = 0,
    ) : KitGrantOutcome

    public data object AlreadyProcessed : KitGrantOutcome

    public data object PreviouslyUncertain : KitGrantOutcome

    public data object PlayerOffline : KitGrantOutcome

    public data object KitNotFound : KitGrantOutcome

    public data object InventoryFull : KitGrantOutcome

    public data object NotAuthorized : KitGrantOutcome

    public data object DeliveryFailed : KitGrantOutcome

    public data object StorageUnavailable : KitGrantOutcome

}

/**
 * Kit catalog/availability reads, ordinary player claims, and authorized system grants.
 *
 * [claimAsPlayer] reuses the existing claim path (Once/Cooldown, inventory precheck, RESERVED then
 * DELIVERED). [grantByAddon] is a distinct, authorized reward path with a durable idempotency key; it
 * never consumes a player's own claim opportunity.
 */
public interface KitsApi {

    public fun listFor(
        actorId: UUID,
        callback: ApiCallback<KitListOutcome>,
    ): ApiRequestHandle

    public fun availability(
        actorId: UUID,
        kitName: String,
        callback: ApiCallback<KitAvailabilityOutcome>,
    ): ApiRequestHandle

    public fun claimAsPlayer(
        actorId: UUID,
        kitName: String,
        callback: ApiCallback<KitClaimOutcome>,
    ): ApiRequestHandle

    public fun grantByAddon(
        request: AddonKitGrantRequest,
        callback: ApiCallback<KitGrantOutcome>,
    ): ApiRequestHandle

    public val events: KitEvents

}
