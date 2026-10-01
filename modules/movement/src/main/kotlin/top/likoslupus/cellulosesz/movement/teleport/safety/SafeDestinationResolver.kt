package top.likoslupus.cellulosesz.movement.teleport.safety

import top.likoslupus.cellulosesz.movement.config.TeleportSafetySettings
import top.likoslupus.cellulosesz.movement.teleport.SafetyMode
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import kotlin.math.floor

/**
 * Deterministic nearest-safe search. It never leaves the configured vertical/horizontal bounds and
 * stops after a hard candidate cap, so an unsafe request cannot turn into an unbounded world scan.
 */
internal class SafeDestinationResolver {

    fun resolve(
        requested: StoredPosition,
        mode: SafetyMode,
        settings: TeleportSafetySettings,
        probe: TeleportProbe,
    ): SafetyResult {
        if (mode == SafetyMode.ALLOW_UNSAFE || !settings.enabled) {
            return SafetyResult.Safe(requested)
        }
        if (isSafe(requested, settings, probe)) {
            return SafetyResult.Safe(requested)
        }
        if (mode == SafetyMode.REQUIRE_SAFE) {
            return SafetyResult.Unsafe
        }

        var candidates = 0
        for ((dx, dz) in horizontalOffsets(settings.searchHorizontalRadius)) {
            for (dy in verticalOffsets(settings.searchVerticalRadius)) {
                if (dx == 0 && dz == 0 && dy == 0) {
                    continue
                }

                candidates += 1
                if (candidates > MAX_CANDIDATES) {
                    return SafetyResult.Unsafe
                }

                val candidate = requested.copy(
                    x = requested.x + dx,
                    y = requested.y + dy,
                    z = requested.z + dz,
                )
                if (isSafe(candidate, settings, probe)) {
                    return SafetyResult.Safe(candidate)
                }
            }
        }

        return SafetyResult.Unsafe
    }

    private fun isSafe(
        position: StoredPosition,
        settings: TeleportSafetySettings,
        probe: TeleportProbe,
    ): Boolean =
        probe.isWithinBorder(position.x, position.z) &&
                probe.isWithinHeight(position.y) &&
                probe.isChunkLoaded(
                    floor(position.x).toInt(),
                    floor(position.z).toInt(),
                ) &&
                when (probe.classify(position.x, position.y, position.z)) {
                    SurfaceKind.SAFE -> true
                    SurfaceKind.WATER -> settings.allowWater
                    else -> false
                }

    private fun horizontalOffsets(radius: Int): Sequence<Pair<Int, Int>> =
        sequence {
            yield(0 to 0)
            for (r in 1..radius) {
                for (dx in -r..r) {
                    for (dz in -r..r) {
                        if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) == r) {
                            yield(dx to dz)
                        }
                    }
                }
            }
        }

    private fun verticalOffsets(radius: Int): Sequence<Int> =
        sequence {
            yield(0)
            for (d in 1..radius) {
                yield(d)
                yield(-d)
            }
        }

    private companion object {

        const val MAX_CANDIDATES: Int = 2048

    }

}
