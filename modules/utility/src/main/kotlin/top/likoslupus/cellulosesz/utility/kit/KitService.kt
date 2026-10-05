package top.likoslupus.cellulosesz.utility.kit

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CancellationException
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.config.KitSettings
import java.time.Clock
import java.time.Duration
import java.util.*

internal sealed interface ClaimKitResult {

    data class Delivered(
        val kit: KitName,
        val droppedStacks: Int,
        val claimRecordedFully: Boolean,
    ) : ClaimKitResult

    data object Disabled : ClaimKitResult
    data object CatalogLoading : ClaimKitResult
    data object StorageUnavailable : ClaimKitResult
    data object InvalidName : ClaimKitResult
    data object NotFound : ClaimKitResult
    data object PlayerOffline : ClaimKitResult
    data object InventoryFull : ClaimKitResult
    data class Cooldown(val remaining: Duration) : ClaimKitResult
    data object AlreadyUsed : ClaimKitResult
    data object DeliveryFailedAfterReservation : ClaimKitResult

}

internal sealed interface CreateKitResult {

    data class Created(val definition: KitDefinition) : CreateKitResult
    data object Disabled : CreateKitResult
    data object InvalidName : CreateKitResult
    data object AlreadyExists : CreateKitResult
    data object InvalidCooldown : CreateKitResult
    data object CooldownTooLong : CreateKitResult
    data object EmptyInventory : CreateKitResult
    data object TooManyItems : CreateKitResult
    data object KitLimitReached : CreateKitResult
    data object PlayerOffline : CreateKitResult
    data object StorageUnavailable : CreateKitResult

}

internal sealed interface UpdateKitResult {

    data class Updated(val definition: KitDefinition) : UpdateKitResult
    data object Disabled : UpdateKitResult
    data object InvalidName : UpdateKitResult
    data object NotFound : UpdateKitResult
    data object InvalidCooldown : UpdateKitResult
    data object CooldownTooLong : UpdateKitResult
    data object EmptyInventory : UpdateKitResult
    data object TooManyItems : UpdateKitResult
    data object PlayerOffline : UpdateKitResult
    data object StorageUnavailable : UpdateKitResult

}

internal sealed interface DeleteKitResult {

    data class Deleted(val name: KitName) : DeleteKitResult
    data object Disabled : DeleteKitResult
    data object InvalidName : DeleteKitResult
    data object NotFound : DeleteKitResult
    data object StorageUnavailable : DeleteKitResult

}

internal sealed interface KitAvailability {

    data object Available : KitAvailability

    data class Cooldown(val remaining: Duration) : KitAvailability

    data object Used : KitAvailability

}

internal data class KitListEntry(
    val name: KitName,
    val reuse: KitReusePolicy,
    val availability: KitAvailability,
)

internal sealed interface ListKitsResult {

    data class Listed(val entries: List<KitListEntry>) : ListKitsResult
    data object Loading : ListKitsResult
    data object StorageUnavailable : ListKitsResult

}

internal sealed interface ShowKitResult {

    data class Shown(val definition: KitDefinition) : ShowKitResult
    data object Disabled : ShowKitResult
    data object Loading : ShowKitResult
    data object StorageUnavailable : ShowKitResult
    data object InvalidName : ShowKitResult
    data object NotFound : ShowKitResult

}

internal sealed interface ResetKitResult {

    data class Reset(
        val name: KitName,
        val target: KnownPlayerIdentity,
    ) : ResetKitResult

    data object NothingToReset : ResetKitResult
    data object Disabled : ResetKitResult
    data object InvalidName : ResetKitResult
    data object NotFound : ResetKitResult
    data object StorageUnavailable : ResetKitResult

}

/**
 * The kit catalog and claim transaction. Claiming reserves durable history before delivery so a
 * crash cannot duplicate items; reservations are never rolled back after a partial delivery.
 */
internal class KitService(
    private val runner: ServerThreadRunner,
    private val definitions: KitRepository,
    private val claims: KitClaimRepository,
    private val inventory: KitInventoryBackend,
    private val known: KnownPlayerResolver,
    private val claimLocks: KeyedMutex<UUID>,
    private val settings: () -> KitSettings,
    private val clock: Clock,
) {

    private val catalog = HashMap<KitName, KitDefinition>()

    @Volatile private var state: KitCatalogState = KitCatalogState.Loading

    fun beginLoad() {
        state = KitCatalogState.Loading
    }

    fun shutdown() {
        catalog.clear()
        state = KitCatalogState.Loading
    }

    suspend fun load() {
        val loaded = try {
            definitions.loadAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to load the kit catalog", exception)
            state = KitCatalogState.Failed(exception.message ?: "unknown error")
            return
        }

        runner.run {
            catalog.clear()
            catalog.putAll(loaded)
            state = KitCatalogState.Ready
        }
    }

    /** Server-thread confined suggestion snapshot for Brigadier. */
    fun configuredNames(): List<String> =
        catalog.keys.map { it.value }.sorted()

    suspend fun claim(playerId: UUID, rawName: String): ClaimKitResult {
        val config = settings()
        if (!config.enabled)
            return ClaimKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return ClaimKitResult.InvalidName

        val snapshot = runner.run { state to catalog[name] }
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
                config.overflowPolicy
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
                )
            }
        }
    }

    private suspend fun loadClaims(playerId: UUID): Map<KitName, KitClaim>? =
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

    suspend fun create(
        playerId: UUID,
        rawName: String,
        reuse: KitReusePolicy,
    ): CreateKitResult {
        val config = settings()
        if (!config.enabled)
            return CreateKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return CreateKitResult.InvalidName
        if (reuse.cooldownTooLong(config)) {
            return CreateKitResult.CooldownTooLong
        }

        when (runner.run { state }) {
            KitCatalogState.Loading -> return CreateKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return CreateKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        if (runner.run { catalog.size } >= config.maxKits) {
            return CreateKitResult.KitLimitReached
        }

        val items = capture(playerId, config)
            ?: return CreateKitResult.PlayerOffline

        when (items) {
            CaptureOutcome.Empty -> return CreateKitResult.EmptyInventory
            CaptureOutcome.TooMany -> return CreateKitResult.TooManyItems
            is CaptureOutcome.Items -> Unit
        }

        val captured = items.items
        val definition = KitDefinition(
            id = KitId(UUID.randomUUID()),
            name = name,
            reuse = reuse,
            items = captured,
        )

        return try {
            if (!definitions.create(definition)) {
                CreateKitResult.AlreadyExists
            } else {
                runner.run { catalog[name] = definition }
                CreateKitResult.Created(definition)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to create kit {}", name.value, exception)
            CreateKitResult.StorageUnavailable
        }
    }

    suspend fun update(
        playerId: UUID,
        rawName: String,
        reuse: KitReusePolicy?,
    ): UpdateKitResult {
        val config = settings()
        if (!config.enabled)
            return UpdateKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return UpdateKitResult.InvalidName

        val current = runner.run { state to catalog[name] }
        when (current.first) {
            KitCatalogState.Loading -> return UpdateKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return UpdateKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        val existing = current.second
            ?: return UpdateKitResult.NotFound
        val policy = reuse ?: existing.reuse
        if (policy.cooldownTooLong(config))
            return UpdateKitResult.CooldownTooLong
        val items = capture(playerId, config)
            ?: return UpdateKitResult.PlayerOffline

        when (items) {
            CaptureOutcome.Empty -> return UpdateKitResult.EmptyInventory
            CaptureOutcome.TooMany -> return UpdateKitResult.TooManyItems
            is CaptureOutcome.Items -> Unit
        }

        val captured = items.items
        val definition = existing.copy(
            reuse = policy,
            items = captured,
        )

        return try {
            if (!definitions.replace(definition)) {
                UpdateKitResult.NotFound
            } else {
                runner.run { catalog[name] = definition }
                UpdateKitResult.Updated(definition)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to update kit {}", name.value, exception)
            UpdateKitResult.StorageUnavailable
        }
    }

    suspend fun delete(rawName: String): DeleteKitResult {
        val config = settings()
        if (!config.enabled)
            return DeleteKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return DeleteKitResult.InvalidName

        when (runner.run { state }) {
            KitCatalogState.Loading -> return DeleteKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return DeleteKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        if (runner.run { catalog[name] } == null) {
            return DeleteKitResult.NotFound
        }

        return try {
            if (!definitions.remove(name)) {
                DeleteKitResult.NotFound
            } else {
                runner.run { catalog.remove(name) }
                DeleteKitResult.Deleted(name)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to delete kit {}", name.value, exception)
            DeleteKitResult.StorageUnavailable
        }
    }

    suspend fun list(playerId: UUID?): ListKitsResult {
        val snapshot = runner.run { state to HashMap(catalog) }
        when (snapshot.first) {
            KitCatalogState.Loading -> return ListKitsResult.Loading
            is KitCatalogState.Failed -> return ListKitsResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        val playerClaims = when {
            playerId != null -> loadClaims(playerId)
                ?: return ListKitsResult.StorageUnavailable

            else -> emptyMap()
        }
        val now = clock.instant()

        val entries = snapshot.second.values
                .sortedBy { it.name.value }
                .map { definition ->
                    KitListEntry(
                        name = definition.name,
                        reuse = definition.reuse,
                        availability = availability(
                            definition,
                            playerClaims[definition.name],
                            now
                        ),
                    )
                }
        return ListKitsResult.Listed(entries)
    }

    suspend fun show(rawName: String): ShowKitResult {
        val config = settings()
        if (!config.enabled)
            return ShowKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return ShowKitResult.InvalidName

        val snapshot = runner.run { state to catalog[name] }
        when (snapshot.first) {
            KitCatalogState.Loading -> return ShowKitResult.Loading
            is KitCatalogState.Failed -> return ShowKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        val kit = snapshot.second
            ?: return ShowKitResult.NotFound
        return ShowKitResult.Shown(kit)
    }

    suspend fun reset(
        rawName: String,
        target: KnownPlayerIdentity,
    ): ResetKitResult {
        val config = settings()
        if (!config.enabled)
            return ResetKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return ResetKitResult.InvalidName
        when (runner.run { state }) {
            KitCatalogState.Loading -> return ResetKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return ResetKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        if (runner.run { catalog[name] } == null) {
            return ResetKitResult.NotFound
        }

        return try {
            when {
                claims.reset(target.id, name) -> ResetKitResult.Reset(name, target)
                else -> ResetKitResult.NothingToReset
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to reset kit {} for {}", name.value, target.id, exception)
            ResetKitResult.StorageUnavailable
        }
    }

    private fun availability(
        definition: KitDefinition,
        claim: KitClaim?,
        now: java.time.Instant,
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

    private suspend fun capture(
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

    private fun KitReusePolicy.cooldownTooLong(config: KitSettings): Boolean {
        val limit = config.maxCooldownSeconds
            ?: return false
        val seconds = (this as? KitReusePolicy.Cooldown)?.duration?.seconds
            ?: return false
        return seconds > limit
    }

    private sealed interface CaptureOutcome {

        data object Empty : CaptureOutcome

        data object TooMany : CaptureOutcome

        data class Items(val items: List<ItemStack>) : CaptureOutcome

    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
