package top.likoslupus.cellulosesz.utility.integration

import java.time.Duration
import java.time.Instant
import java.util.*

/** How often a kit may be reused, projected Minecraft-free for the public API. */
public sealed interface KitReuseView {

    public data object Always : KitReuseView

    public data object Once : KitReuseView

    public data class Cooldown(public val duration: Duration) : KitReuseView

}

/** Whether a specific player may claim a kit right now. */
public sealed interface KitAvailabilityView {

    public data object Available : KitAvailabilityView

    public data object AlreadyUsed : KitAvailabilityView

    public data class Cooldown(public val remaining: Duration) : KitAvailabilityView

}

public class KitSummary(
    public val name: String,
    public val reuse: KitReuseView,
    public val availability: KitAvailabilityView,
)

public sealed interface KitListResult {

    public data class Listed(public val kits: List<KitSummary>) : KitListResult

    public data object Disabled : KitListResult

    public data object Loading : KitListResult

    public data object StorageUnavailable : KitListResult

}

public sealed interface KitAvailabilityResult {

    public data class Found(public val availability: KitAvailabilityView) : KitAvailabilityResult

    public data object NotFound : KitAvailabilityResult

    public data object Disabled : KitAvailabilityResult

    public data object Loading : KitAvailabilityResult

    public data object StorageUnavailable : KitAvailabilityResult

}

public sealed interface KitClaimResult {

    public data class Delivered(
        public val kitName: String,
        public val droppedStacks: Int,
        public val claimRecordedFully: Boolean,
    ) : KitClaimResult

    public data object Disabled : KitClaimResult

    public data object Loading : KitClaimResult

    public data object NotFound : KitClaimResult

    public data object InvalidName : KitClaimResult

    public data class Cooldown(public val remaining: Duration) : KitClaimResult

    public data object AlreadyUsed : KitClaimResult

    public data object InventoryFull : KitClaimResult

    public data object PlayerOffline : KitClaimResult

    public data object StorageUnavailable : KitClaimResult

    /** Items were reserved but final delivery could not be confirmed — never auto-retried. */
    public data object DeliveryFailedAfterReservation : KitClaimResult

    /** An eligibility checker denied the claim before any precheck or reservation. */
    public data class Denied(public val reasonKey: String) : KitClaimResult

}

/**
 * A durable system reward request. [grantId] is the addon-supplied reward identity and the durable
 * de-duplication key. Authorization (the `kit.grant` capability of the requesting addon) is decided
 * by the application adapter, so this port stays addon-agnostic.
 */
public class KitGrantRequest(
    public val addonId: String,
    public val playerId: UUID,
    public val kitName: String,
    public val grantId: String,
)

public sealed interface KitGrantResult {

    public data class Delivered(
        public val grantId: String,
        public val kitName: String,
        public val droppedStacks: Int,
    ) : KitGrantResult

    public data object AlreadyProcessed : KitGrantResult

    public data object PreviouslyUncertain : KitGrantResult

    public data object PlayerOffline : KitGrantResult

    public data object KitNotFound : KitGrantResult

    public data object InventoryFull : KitGrantResult

    public data object DeliveryFailed : KitGrantResult

    public data object StorageUnavailable : KitGrantResult

    public data object Disabled : KitGrantResult

    public data object Loading : KitGrantResult

}

/** Coarse catalog mutation kind for the integration hook; distinct from the public projection. */
public enum class KitCatalogChangeKind {

    CREATED,
    UPDATED,
    DELETED,

}

/** A committed kit-catalog mutation fact (no item data). */
public class KitCatalogChangeEvent(
    public val kitName: String,
    public val change: KitCatalogChangeKind,
    public val occurredAt: Instant,
)

public fun interface KitCatalogObserver {

    public fun onCatalogChanged(event: KitCatalogChangeEvent)

}

public fun interface KitCatalogSubscription {

    public fun close()

}

/**
 * The narrow integration seam the application kit adapter uses to reach the utility bounded context.
 *
 * It exposes the operations the public API needs and nothing else: it never exposes the internal
 * kit service, its catalog, or its repositories. Catalog and availability reads and ordinary claims
 * reuse the exact command path (enabled check, catalog readiness, Once/Cooldown, inventory
 * preflight, RESERVED then DELIVERED). [grant] is the distinct, durable, idempotent system-reward
 * path.
 */
public interface KitIntegrationPort {

    public suspend fun list(playerId: UUID): KitListResult

    public suspend fun availability(playerId: UUID, kitName: String): KitAvailabilityResult

    public suspend fun claim(playerId: UUID, kitName: String): KitClaimResult

    public suspend fun grant(request: KitGrantRequest): KitGrantResult

    /** Registers a catalog-change observer; close the returned handle to unregister it. */
    public fun observeCatalogChanges(observer: KitCatalogObserver): KitCatalogSubscription

}
