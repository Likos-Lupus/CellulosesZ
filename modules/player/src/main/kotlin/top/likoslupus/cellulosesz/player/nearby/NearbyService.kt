package top.likoslupus.cellulosesz.player.nearby

import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.player.config.NearbySettings
import java.util.*
import kotlin.math.sqrt

/** A player close to the caller, with the horizontal distance in blocks. */
internal data class NearbyPlayer(
    val id: UUID,
    val name: String,
    val distance: Double,
)

/** Values-only candidate so the selection rule can be unit tested without Minecraft. */
internal data class NearbyCandidate(
    val id: UUID,
    val name: String,
    val dimension: String,
    val x: Double,
    val z: Double,
    val spectator: Boolean,
)

/** The outcome of a `/near` query. */
internal sealed interface NearbyResult {

    data class Found(val players: List<NearbyPlayer>, val radius: Int) : NearbyResult

    data object PlayerOffline : NearbyResult

    /** The requested radius exceeds the configured maximum and the caller lacks the bypass. */
    data class RadiusTooLarge(val maximum: Int) : NearbyResult

}

/** Pure selection rule for nearby players: same dimension, within radius, capped and sorted. */
internal object NearbyFilter {

    fun select(
        candidates: List<NearbyCandidate>,
        originId: UUID,
        dimension: String,
        originX: Double,
        originZ: Double,
        radius: Double,
        maximumResults: Int,
        hideSpectators: Boolean,
    ): List<NearbyPlayer> =
        candidates.asSequence()
                .filterNot { it.id == originId }
                .filter { it.dimension == dimension }
                .filterNot { hideSpectators && it.spectator }
                .map {
                    it to distance(
                        originX,
                        originZ,
                        it.x,
                        it.z
                    )
                }
                .filter { (_, distance) -> distance <= radius }
                .sortedBy { (_, distance) -> distance }
                .take(maximumResults.coerceAtLeast(1))
                .map { (candidate, distance) ->
                    NearbyPlayer(candidate.id, candidate.name, distance)
                }
                .toList()

    private fun distance(
        ax: Double,
        az: Double,
        bx: Double,
        bz: Double
    ): Double {
        val dx = ax - bx
        val dz = az - bz
        return sqrt(dx * dx + dz * dz)
    }

}

/**
 * Server-thread confined nearby lookup. Radius is clamped to the configured maximum so a caller
 * can never widen the query beyond what the server allows. A caller holding the bypass capability
 * (the command resolves that) may exceed the maximum.
 */
internal class NearbyService(
    private val kernel: RuntimeKernel,
    private val settings: () -> NearbySettings,
) {

    fun nearby(
        playerId: UUID,
        requestedRadius: Int?,
        allowOverMaximum: Boolean = false,
    ): NearbyResult {
        val server = kernel.requireServer()
        val origin = server.playerList.getPlayer(playerId)
            ?: return NearbyResult.PlayerOffline
        val maximum = settings().maximumRadius
        if (requestedRadius != null
            && requestedRadius > maximum
            && !allowOverMaximum
        ) {
            return NearbyResult.RadiusTooLarge(maximum)
        }
        val radius = resolveRadius(requestedRadius, maximum, allowOverMaximum)
        val candidates = server.playerList.players.map { player ->
            NearbyCandidate(
                id = player.uuid,
                name = player.gameProfile.name,
                dimension = player.level().dimension().identifier().toString(),
                x = player.x,
                z = player.z,
                spectator = player.isSpectator,
            )
        }
        val players = NearbyFilter.select(
            candidates = candidates,
            originId = origin.uuid,
            dimension = origin.level().dimension().identifier().toString(),
            originX = origin.x,
            originZ = origin.z,
            radius = radius.toDouble(),
            maximumResults = settings().maximumResults,
            hideSpectators = settings().hideSpectators,
        )
        return NearbyResult.Found(players, radius)
    }

    private fun resolveRadius(
        requested: Int?,
        maximum: Int,
        allowOverMaximum: Boolean,
    ): Int =
        when {
            requested == null -> settings().defaultRadius.coerceIn(1, maximum)
            allowOverMaximum -> requested.coerceAtLeast(1)
            else -> requested.coerceIn(1, maximum)
        }

}
