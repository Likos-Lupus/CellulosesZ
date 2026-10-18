package top.likoslupus.cellulosesz.utility.kit

import com.mojang.logging.LogUtils
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.config.KitSettings
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlinx.coroutines.CancellationException

/**
 * The claim transaction: eligibility, inventory preflight, the durable reservation and the
 * delivery. A reservation is never rolled back after a partial delivery, so a crash cannot
 * duplicate items.
 */
internal class KitClaimEngine(
    private val runner: ServerThreadRunner,
    private val claims: KitClaimRepository,
    private val inventory: KitInventoryBackend,
    private val known: KnownPlayerResolver,
    private val claimLocks: KeyedMutex<UUID>,
    private val settings: () -> KitSettings,
    private val clock: Clock,
    private val catalog: KitCatalog,
    private val eligibility: KitClaimEligibility = KitClaimEligibility.AllowAll,
) {

    suspend fun claim(playerId: UUID, rawName: String): ClaimKitResult {
        val config = settings()
        if (!config.enabled)
            return ClaimKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return ClaimKitResult.InvalidName

        val snapshot = catalog.snapshot(name)
        when (snapshot.first) {
            KitCatalogState.Loading -> return ClaimKitResult.CatalogLoading
            is KitCatalogState.Failed -> return ClaimKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        val kit = snapshot.second
            ?: return ClaimKitResult.NotFound

        return claimLocks.withLock(playerId) {
            claimLocked(playerId, kit, config)
        }
    }

    private suspend fun claimLocked(
        playerId: UUID,
        kit: KitDefinition,
        config: KitSettings,
    ): ClaimKitResult {
        val identity = runner.run { known.onlineById(playerId) }
            ?: return ClaimKitResult.PlayerOffline

        if (kit.reuse != KitReusePolicy.Always) {
            val existing = loadClaims(playerId)
                ?: return ClaimKitResult.StorageUnavailable
            val claim = existing[kit.name]

            if (claim != null
                && claim.kitId == kit.id.value
            ) when (val policy = kit.reuse) {
                KitReusePolicy.Once -> return ClaimKitResult.AlreadyUsed

                is KitReusePolicy.Cooldown -> {
                    val now = clock.instant()
                    val availableAt = claim.claimedAt.plus(policy.duration)
                    if (now.isBefore(availableAt)) {
                        return ClaimKitResult.Cooldown(
                            Duration.between(now, availableAt)
                        )
                    }
                }

                KitReusePolicy.Always -> Unit
            }

        }

        when (val decision = eligibility.evaluate(playerId, kit.name.value)) {
            KitEligibilityDecision.Allow -> Unit
            is KitEligibilityDecision.Deny ->
                return ClaimKitResult.Denied(decision.reasonKey)
        }

        when (
            runner.run {
                inventory.preflight(
                    playerId,
                    kit.items,
                    config.overflowPolicy
                )
            }
        ) {
            KitPreflightResult.PlayerOffline -> return ClaimKitResult.PlayerOffline
            KitPreflightResult.Full -> return ClaimKitResult.InventoryFull
            KitPreflightResult.Fits -> Unit
        }

        val needsClaim = kit.reuse != KitReusePolicy.Always
        if (needsClaim
            && !reserve(identity, kit)
        ) {
            return ClaimKitResult.StorageUnavailable
        }

        val delivery = runner.run {
            inventory.deliver(
                playerId,
                kit.items,
                config.overflowPolicy,
                config.autoEquip,
            )
        }

        when (delivery) {
            KitDeliveryResult.PlayerOffline ->
                return when {
                    needsClaim -> ClaimKitResult.DeliveryFailedAfterReservation
                    else -> ClaimKitResult.PlayerOffline
                }

            KitDeliveryResult.Failed ->
                return when {
                    needsClaim -> ClaimKitResult.DeliveryFailedAfterReservation
                    else -> ClaimKitResult.PlayerOffline
                }

            is KitDeliveryResult.Delivered -> {
                val recorded = !needsClaim || finalizeDelivered(identity, kit)
                return ClaimKitResult.Delivered(
                    kit = kit.name,
                    droppedStacks = delivery.droppedStacks,
                    claimRecordedFully = recorded,
                    equippedStacks = delivery.equippedStacks,
                )
            }
        }
    }

    /** Durable claims for [playerId], or `null` when the read failed. */
    suspend fun loadClaims(playerId: UUID): Map<KitName, KitClaim>? =
        try {
            claims.load(playerId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to read kit claims for {}", playerId, exception)
            null
        }

    private suspend fun reserve(
        identity: KnownPlayerIdentity,
        kit: KitDefinition
    ): Boolean =
        try {
            claims.reserve(identity, kit, clock.instant())
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to reserve claim for {} on kit {}",
                identity.id,
                kit.name.value,
                exception,
            )
            false
        }

    private suspend fun finalizeDelivered(
        identity: KnownPlayerIdentity,
        kit: KitDefinition,
    ): Boolean =
        try {
            claims.markDelivered(identity, kit, clock.instant())
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to finalize claim for {} on kit {}",
                identity.id,
                kit.name.value,
                exception,
            )
            false
        }

    /** Captures the actor's inventory into a candidate kit payload, or `null` when offline. */
    suspend fun capture(
        playerId: UUID,
        config: KitSettings,
    ): CaptureOutcome? =
        when (val result = runner.run { inventory.capture(playerId) }) {
            KitCaptureResult.PlayerOffline -> null
            is KitCaptureResult.Captured ->
                when {
                    result.items.isEmpty() -> CaptureOutcome.Empty
                    result.items.size > config.maxItemsPerKit -> CaptureOutcome.TooMany
                    else -> CaptureOutcome.Items(result.items)
                }
        }

    /** Availability of [definition] for a player holding [claim]. */
    fun availability(
        definition: KitDefinition,
        claim: KitClaim?,
        now: Instant,
    ): KitAvailability =
        when {
            claim == null || claim.kitId != definition.id.value ->
                KitAvailability.Available

            else -> when (val policy = definition.reuse) {
                KitReusePolicy.Always -> KitAvailability.Available
                KitReusePolicy.Once -> KitAvailability.Used
                is KitReusePolicy.Cooldown -> {
                    val availableAt = claim.claimedAt.plus(policy.duration)
                    when {
                        now.isBefore(availableAt) ->
                            KitAvailability.Cooldown(
                                Duration.between(now, availableAt)
                            )

                        else -> KitAvailability.Available
                    }
                }
            }
        }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}

/** The payload captured from an inventory, classified against the kit limits. */
internal sealed interface CaptureOutcome {

    data object Empty : CaptureOutcome

    data object TooMany : CaptureOutcome

    data class Items(val items: List<ItemStack>) : CaptureOutcome

}
