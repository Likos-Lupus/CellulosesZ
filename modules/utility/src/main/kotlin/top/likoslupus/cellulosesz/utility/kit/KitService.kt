package top.likoslupus.cellulosesz.utility.kit

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.utility.config.KitSettings
import java.time.Clock
import java.util.*
import kotlinx.coroutines.CancellationException

/**
 * The kit catalog and claim transaction facade. Claiming reserves durable history before delivery
 * so a crash cannot duplicate items; reservations are never rolled back after a partial delivery.
 */
internal class KitService(
    runner: ServerThreadRunner,
    private val definitions: KitRepository,
    private val claims: KitClaimRepository,
    inventory: KitInventoryBackend,
    known: KnownPlayerResolver,
    claimLocks: KeyedMutex<UUID>,
    private val settings: () -> KitSettings,
    private val clock: Clock,
    eligibility: KitClaimEligibility = KitClaimEligibility.AllowAll,
) {

    private val catalog = KitCatalog(runner, clock)

    private val claimEngine = KitClaimEngine(
        runner = runner,
        claims = claims,
        inventory = inventory,
        known = known,
        claimLocks = claimLocks,
        settings = settings,
        clock = clock,
        catalog = catalog,
        eligibility = eligibility,
    )

    private val editor = KitCatalogEditor(
        definitions = definitions,
        capture = { playerId, config -> claimEngine.capture(playerId, config) },
        settings = settings,
        catalog = catalog,
    )

    /** Product policy gate shared by the command path and the integration port. */
    fun enabled(): Boolean = settings().enabled

    /** Registers a catalog-mutation observer; close the handle to unregister it. */
    fun observeCatalogChanges(listener: KitCatalogListener): KitCatalogRegistration =
        catalog.observe(listener)

    /** Server-thread-confined catalog resolution, distinguishing missing from not-yet-loaded. */
    suspend fun lookup(name: KitName): KitLookup =
        catalog.lookup(name)

    fun beginLoad() {
        catalog.beginLoad()
    }

    fun shutdown() {
        catalog.shutdown()
    }

    suspend fun load() {
        val loaded = try {
            definitions.loadAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error("failed to load the kit catalog", exception)
            catalog.fail(exception.message ?: "unknown error")
            return
        }

        catalog.install(loaded)
    }

    /** Server-thread confined suggestion snapshot for Brigadier. */
    fun configuredNames(): List<String> =
        catalog.configuredNames()

    suspend fun claim(playerId: UUID, rawName: String): ClaimKitResult =
        claimEngine.claim(playerId, rawName)

    suspend fun create(
        playerId: UUID,
        rawName: String,
        reuse: KitReusePolicy,
    ): CreateKitResult =
        editor.create(playerId, rawName, reuse)

    suspend fun update(
        playerId: UUID,
        rawName: String,
        reuse: KitReusePolicy?,
    ): UpdateKitResult =
        editor.update(playerId, rawName, reuse)

    suspend fun delete(rawName: String): DeleteKitResult =
        editor.delete(rawName)

    suspend fun list(playerId: UUID?): ListKitsResult {
        val snapshot = catalog.snapshotAll()
        when (snapshot.first) {
            KitCatalogState.Loading -> return ListKitsResult.Loading
            is KitCatalogState.Failed -> return ListKitsResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        val playerClaims = when {
            playerId != null -> claimEngine.loadClaims(playerId)
                ?: return ListKitsResult.StorageUnavailable

            else -> emptyMap()
        }
        val now = clock.instant()
        val config = settings()

        val entries = snapshot.second.values
                .sortedBy { it.name.value }
                .map { definition ->
                    KitListEntry(
                        name = definition.name,
                        reuse = definition.reuse,
                        availability = claimEngine.availability(
                            definition,
                            playerClaims[definition.name],
                            now
                        ),
                    )
                }
                .filter { entry ->
                    !config.hideClaimedOnce
                            || entry.reuse != KitReusePolicy.Once
                            || entry.availability != KitAvailability.Used
                }
        return ListKitsResult.Listed(entries)
    }

    suspend fun show(rawName: String): ShowKitResult {
        val config = settings()
        if (!config.enabled)
            return ShowKitResult.Disabled
        val name = KitName.parse(rawName)
            ?: return ShowKitResult.InvalidName

        val snapshot = catalog.snapshot(name)
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
        val snapshot = catalog.snapshot(name)
        when (snapshot.first) {
            KitCatalogState.Loading -> return ResetKitResult.StorageUnavailable
            is KitCatalogState.Failed -> return ResetKitResult.StorageUnavailable
            KitCatalogState.Ready -> Unit
        }

        if (snapshot.second == null) {
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

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
