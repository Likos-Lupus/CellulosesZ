package top.likoslupus.cellulosesz.utility.kit

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.config.KitSettings
import java.time.Clock
import java.util.*
import kotlinx.coroutines.CancellationException

/** The domain result of a durable system grant. Mapped to the public projection by the adapter. */
internal sealed interface GrantKitResult {

    data class Delivered(
        val grantId: String,
        val kitName: KitName,
        val droppedStacks: Int,
    ) : GrantKitResult

    data object AlreadyProcessed : GrantKitResult
    data object PreviouslyUncertain : GrantKitResult
    data object PlayerOffline : GrantKitResult
    data object KitNotFound : GrantKitResult
    data object InventoryFull : GrantKitResult
    data object DeliveryFailed : GrantKitResult
    data object StorageUnavailable : GrantKitResult
    data object Disabled : GrantKitResult
    data object Loading : GrantKitResult

}

/** Composite de-duplication key; serializes concurrent grants of the same reward instance. */
private data class GrantKey(
    val addonId: String,
    val grantId: String,
    val playerId: UUID,
)

/**
 * The durable, idempotent system-reward path. It is deliberately distinct from [KitService.claim]:
 * a grant never consumes a player's own claim opportunity, and its de-duplication key is the
 * caller-supplied [grantId] rather than the kit identity.
 *
 * The ledger is written [KitGrantStatus.RESERVED] before items are delivered. A crash between the
 * two leaves the grant uncertain; a later call with the same key returns
 * [GrantKitResult.PreviouslyUncertain] and **never** re-delivers. Only a fully delivered grant
 * returns [GrantKitResult.AlreadyProcessed] on retry.
 */
internal class KitGrantService(
    private val runner: ServerThreadRunner,
    private val kits: KitService,
    private val grants: KitGrantRepository,
    private val inventory: KitInventoryBackend,
    private val known: KnownPlayerResolver,
    private val settings: () -> KitSettings,
    private val clock: Clock,
) {

    private val grantLocks = KeyedMutex<GrantKey>()

    suspend fun grant(
        addonId: String,
        playerId: UUID,
        rawKitName: String,
        grantId: String,
    ): GrantKitResult {
        val config = settings()
        if (!config.enabled) {
            return GrantKitResult.Disabled
        }
        val name = KitName.parse(rawKitName)
            ?: return GrantKitResult.KitNotFound

        val kit = when (val lookup = kits.lookup(name)) {
            KitLookup.Loading -> return GrantKitResult.Loading
            KitLookup.StorageUnavailable -> return GrantKitResult.StorageUnavailable
            KitLookup.NotFound -> return GrantKitResult.KitNotFound
            is KitLookup.Found -> lookup.definition
        }

        return grantLocks.withLock(GrantKey(addonId, grantId, playerId)) {
            grantLocked(addonId, playerId, grantId, kit, config)
        }
    }

    private suspend fun grantLocked(
        addonId: String,
        playerId: UUID,
        grantId: String,
        kit: KitDefinition,
        config: KitSettings,
    ): GrantKitResult {
        val existing = try {
            grants.find(addonId, grantId, playerId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to read kit grant {} for {}", grantId, playerId, exception)
            return GrantKitResult.StorageUnavailable
        }

        when (existing?.state) {
            KitGrantStatus.DELIVERED -> return GrantKitResult.AlreadyProcessed
            KitGrantStatus.RESERVED -> return GrantKitResult.PreviouslyUncertain
            null -> Unit
        }

        runner.run { known.onlineById(playerId) }
            ?: return GrantKitResult.PlayerOffline

        when (
            runner.run {
                inventory.preflight(
                    playerId,
                    kit.items,
                    config.overflowPolicy
                )
            }
        ) {
            KitPreflightResult.PlayerOffline -> return GrantKitResult.PlayerOffline
            KitPreflightResult.Full -> return GrantKitResult.InventoryFull
            KitPreflightResult.Fits -> Unit
        }

        val reservation = try {
            grants.reserve(
                addonId,
                playerId,
                kit.name,
                grantId,
                clock.instant(),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to reserve kit grant {} for {}", grantId, playerId, exception)
            return GrantKitResult.StorageUnavailable
        }

        when (reservation) {
            KitGrantReservation.Inserted -> Unit
            is KitGrantReservation.Existing -> return when (reservation.state) {
                KitGrantStatus.DELIVERED -> GrantKitResult.AlreadyProcessed
                KitGrantStatus.RESERVED -> GrantKitResult.PreviouslyUncertain
            }
        }

        val delivery = runner.run {
            inventory.deliver(
                playerId,
                kit.items,
                config.overflowPolicy
            )
        }

        return when (delivery) {
            is KitDeliveryResult.Delivered -> {
                finalize(addonId, playerId, grantId)
                GrantKitResult.Delivered(
                    grantId = grantId,
                    kitName = kit.name,
                    droppedStacks = delivery.droppedStacks,
                )
            }

            KitDeliveryResult.PlayerOffline, KitDeliveryResult.Failed ->
                GrantKitResult.DeliveryFailed
        }
    }

    private suspend fun finalize(
        addonId: String,
        playerId: UUID,
        grantId: String
    ) {
        try {
            if (
                !grants.markDelivered(
                    addonId,
                    grantId,
                    playerId,
                    clock.instant()
                )
            ) {
                LOGGER.warn(
                    "kit grant {} delivery could not be finalized for {}",
                    grantId,
                    playerId,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to finalize kit grant {} for {}", grantId, playerId, exception)
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
