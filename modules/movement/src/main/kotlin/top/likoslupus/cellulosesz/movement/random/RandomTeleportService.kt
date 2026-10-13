package top.likoslupus.cellulosesz.movement.random

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import top.likoslupus.cellulosesz.movement.config.RandomTeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Outcome of a `/tpr` search. Expected failures are values. */
internal sealed interface RandomTeleportResult {

    data class Found(val position: StoredPosition) : RandomTeleportResult

    data object Disabled : RandomTeleportResult

    data object NotConfigured : RandomTeleportResult

    data class UnknownRegion(val name: String) : RandomTeleportResult

    data class WrongDimension(val name: String) : RandomTeleportResult

    data class RegionDisabled(val name: String) : RandomTeleportResult

    data class NoValidArea(val name: String) : RandomTeleportResult

    data object RegionChanged : RandomTeleportResult

    data object SearchFailed : RandomTeleportResult

    data object Busy : RandomTeleportResult

    data class Cooldown(val remaining: Duration) : RandomTeleportResult

    data object PlayerOffline : RandomTeleportResult

}

internal sealed interface SetRegionResult {

    data object Saved : SetRegionResult

    data object InvalidName : SetRegionResult

    data object InvalidRange : SetRegionResult

    data object NotFound : SetRegionResult

    data object WrongDimension : SetRegionResult

    data object LimitReached : SetRegionResult

}

internal sealed interface RegionLookupResult {

    data class Found(val region: RandomTeleportRegion) : RegionLookupResult

    data object InvalidName : RegionLookupResult

    data object NotFound : RegionLookupResult

}

internal sealed interface DeleteRegionResult {

    data object Deleted : DeleteRegionResult

    data object InvalidName : DeleteRegionResult

    data object NotFound : DeleteRegionResult

}

/**
 * Owns the `/tpr` search budget and the `/settpr` region mutations: cooldown, server-wide and
 * per-player concurrency, attempts and timeout. The actual candidate resolution is delegated to
 * [RandomTeleportBackend]; this class holds only transient pacing state and never touches the world.
 */
internal class RandomTeleportService(
    private val regions: RandomTeleportRegionRepository,
    private val backend: RandomTeleportBackend,
    private val planner: RandomTeleportPlanner,
    private val settings: () -> RandomTeleportSettings,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {

    private val lock = Mutex()
    private val activeServer = AtomicInteger()
    private val activePerPlayer = HashMap<UUID, Int>()
    private val lastUse = HashMap<UUID, ComparableTimeMark>()

    suspend fun roll(
        playerId: UUID,
        dimension: String,
        regionName: String?,
    ): RandomTeleportResult {
        val config = settings()
        if (!config.enabled) {
            return RandomTeleportResult.Disabled
        }

        val denied = lock.withLock { acquire(playerId, config) }
        if (denied != null) {
            return denied
        }

        var found = false
        try {
            val region = when (regionName) {
                null -> regions.find(RegionName.DEFAULT)
                    ?: return RandomTeleportResult.NotConfigured

                else -> RegionName.parse(regionName)
                        ?.let { regions.find(it) }
                    ?: return RandomTeleportResult.UnknownRegion(regionName)
            }
            if (region.dimension != dimension) {
                return RandomTeleportResult.WrongDimension(region.name.value)
            }
            if (!region.enabled) {
                return RandomTeleportResult.RegionDisabled(region.name.value)
            }

            val resolution = withTimeoutOrNull(config.searchTimeoutSeconds.seconds) {
                repeat(config.maxAttempts) {
                    val candidate = planner.candidate(region)
                    when (
                        val resolved = backend.resolveSafe(
                            playerId,
                            region,
                            candidate,
                            config,
                        )
                    ) {
                        is RandomResolution.Safe -> return@withTimeoutOrNull resolved
                        RandomResolution.NoValidArea ->
                            return@withTimeoutOrNull RandomResolution.NoValidArea

                        RandomResolution.Unsafe -> Unit
                    }
                }
                null
            }
            val safe = when (resolution) {
                null -> return RandomTeleportResult.SearchFailed

                RandomResolution.NoValidArea ->
                    return RandomTeleportResult.NoValidArea(region.name.value)

                is RandomResolution.Safe -> resolution.position

                RandomResolution.Unsafe -> return RandomTeleportResult.SearchFailed
            }

            val current = regions.find(region.name)
            if (current == null || current.revision != region.revision || !current.enabled) {
                return RandomTeleportResult.RegionChanged
            }

            found = true
            return RandomTeleportResult.Found(safe)
        } finally {
            lock.withLock { release(playerId, found) }
        }
    }

    suspend fun create(
        rawName: String,
        center: StoredPosition,
        minimumRadius: Double,
        maximumRadius: Double,
    ): SetRegionResult {
        val config = settings()
        val name = RegionName.parse(rawName)
            ?: return SetRegionResult.InvalidName
        if (!isValidRange(minimumRadius, maximumRadius, config)) {
            return SetRegionResult.InvalidRange
        }
        if (regions.find(name) == null && regions.count() >= config.maxRegions) {
            return SetRegionResult.LimitReached
        }

        regions.put(
            RandomTeleportRegion(
                name = name,
                dimension = center.dimension,
                centerX = center.x,
                centerZ = center.z,
                minimumRadius = minimumRadius,
                maximumRadius = maximumRadius,
                enabled = true,
                revision = 0L,
            )
        )
        return SetRegionResult.Saved
    }

    suspend fun setCenter(rawName: String, center: StoredPosition): SetRegionResult {
        val name = RegionName.parse(rawName)
            ?: return SetRegionResult.InvalidName
        val existing = regions.find(name)
            ?: return SetRegionResult.NotFound
        if (existing.dimension != center.dimension) {
            return SetRegionResult.WrongDimension
        }

        regions.put(
            existing.copy(
                centerX = center.x,
                centerZ = center.z,
            )
        )
        return SetRegionResult.Saved
    }

    suspend fun setRange(
        rawName: String,
        minimumRadius: Double,
        maximumRadius: Double,
    ): SetRegionResult {
        val config = settings()
        val name = RegionName.parse(rawName)
            ?: return SetRegionResult.InvalidName
        val existing = regions.find(name)
            ?: return SetRegionResult.NotFound
        if (!isValidRange(minimumRadius, maximumRadius, config)) {
            return SetRegionResult.InvalidRange
        }

        regions.put(
            existing.copy(
                minimumRadius = minimumRadius,
                maximumRadius = maximumRadius,
            )
        )
        return SetRegionResult.Saved
    }

    suspend fun removeRegion(rawName: String): DeleteRegionResult {
        val name = RegionName.parse(rawName)
            ?: return DeleteRegionResult.InvalidName
        return when {
            regions.remove(name) -> DeleteRegionResult.Deleted
            else -> DeleteRegionResult.NotFound
        }
    }

    suspend fun info(rawName: String): RegionLookupResult {
        val name = RegionName.parse(rawName)
            ?: return RegionLookupResult.InvalidName
        val region = regions.find(name)
            ?: return RegionLookupResult.NotFound
        return RegionLookupResult.Found(region)
    }

    suspend fun list(): List<RandomTeleportRegion> =
        regions.list()

    private fun isValidRange(
        minimumRadius: Double,
        maximumRadius: Double,
        config: RandomTeleportSettings,
    ): Boolean =
        minimumRadius.isFinite() &&
                maximumRadius.isFinite() &&
                minimumRadius >= 0.0 &&
                minimumRadius < maximumRadius &&
                maximumRadius <= config.maxRadius.toDouble()

    private fun acquire(
        playerId: UUID,
        config: RandomTeleportSettings,
    ): RandomTeleportResult? {
        if (config.cooldownSeconds > 0) {
            val mark = lastUse[playerId]
            if (mark != null) {
                val remaining = config.cooldownSeconds.seconds - mark.elapsedNow()
                if (remaining > Duration.ZERO) {
                    return RandomTeleportResult.Cooldown(remaining)
                }
            }
        }

        if (activeServer.get() >= config.maxConcurrentServerSearches) {
            return RandomTeleportResult.Busy
        }
        if ((activePerPlayer[playerId] ?: 0) >= config.maxConcurrentPerPlayer) {
            return RandomTeleportResult.Busy
        }
        activeServer.incrementAndGet()
        activePerPlayer[playerId] = (activePerPlayer[playerId] ?: 0) + 1
        return null
    }

    private fun release(playerId: UUID, success: Boolean) {
        activeServer.decrementAndGet()
        val remaining = (activePerPlayer[playerId] ?: 1) - 1
        if (remaining <= 0) {
            activePerPlayer.remove(playerId)
        } else {
            activePerPlayer[playerId] = remaining
        }
        if (success) {
            lastUse[playerId] = timeSource.markNow()
        }
    }

}
