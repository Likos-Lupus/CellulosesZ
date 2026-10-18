package top.likoslupus.cellulosesz.administration.playerstate

import top.likoslupus.cellulosesz.administration.config.PlayerAdjustmentSettings
import top.likoslupus.cellulosesz.administration.operator.PlayerGameMode
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

internal sealed interface HealResult {

    data class Healed(val target: String) : HealResult

    data class OnCooldown(val remaining: Duration) : HealResult

    data object TargetOffline : HealResult

}

internal sealed interface GodResult {

    data class Applied(
        val target: String,
        val enabled: Boolean
    ) : GodResult

    data object DisabledWorld : GodResult

    data object TargetOffline : GodResult

}

internal sealed interface FlyResult {

    data class Applied(
        val target: String,
        val enabled: Boolean
    ) : FlyResult

    data object TargetOffline : FlyResult

}

/**
 * Owns `/heal`, `/god` and `/fly` session state and their world/game-mode policy. All state is
 * server-thread confined and in-memory: god is actively tracked (it is not inferred from arbitrary
 * invulnerability) and flight is only re-asserted for operators who set it.
 */
internal class PlayerStateService(
    private val backend: PlayerStateBackend,
    private val known: KnownPlayerResolver,
    private val settings: () -> PlayerAdjustmentSettings,
    private val clock: Clock,
) {

    private val godPlayers = HashSet<UUID>()
    private val flyIntent = HashMap<UUID, Boolean>()
    private val lastHeal = HashMap<UUID, Instant>()
    private val lastWorld = HashMap<UUID, String>()
    private val lastGameMode = HashMap<UUID, PlayerGameMode>()

    fun heal(
        actorId: UUID,
        targetId: UUID,
        bypassCooldown: Boolean
    ): HealResult {
        val target = known.onlineById(targetId)?.name
            ?: return HealResult.TargetOffline
        val now = clock.instant()
        val remaining = HealCooldown.remaining(
            lastHeal[actorId],
            now,
            settings().healCooldownSeconds,
            bypassCooldown,
        )
        if (remaining != null) {
            return HealResult.OnCooldown(remaining)
        }
        if (
            !backend.applyHeal(
                targetId,
                settings().removeEffectsOnHeal
            )
        ) {
            return HealResult.TargetOffline
        }
        lastHeal[actorId] = now
        return HealResult.Healed(target)
    }

    fun toggleGod(targetId: UUID): GodResult {
        val target = known.onlineById(targetId)?.name
            ?: return GodResult.TargetOffline
        val enable = targetId !in godPlayers
        if (enable && isNoGodWorld(backend.dimension(targetId))) {
            return GodResult.DisabledWorld
        }
        if (!backend.setInvulnerable(targetId, enable)) {
            return GodResult.TargetOffline
        }
        if (enable) {
            godPlayers.add(targetId)
        } else {
            godPlayers.remove(targetId)
        }
        return GodResult.Applied(target, enable)
    }

    fun toggleFly(targetId: UUID): FlyResult {
        val target = known.onlineById(targetId)?.name
            ?: return FlyResult.TargetOffline
        val enable = !backend.mayFly(targetId)
        if (!backend.setFly(targetId, enable)) {
            return FlyResult.TargetOffline
        }
        flyIntent[targetId] = enable
        return FlyResult.Applied(target, enable)
    }

    fun isGod(targetId: UUID): Boolean = targetId in godPlayers

    /** Grants flight without an audit; used by `flyOnJoin`. Returns false when offline. */
    fun grantFly(targetId: UUID, enabled: Boolean): Boolean {
        if (!backend.setFly(targetId, enabled)) {
            return false
        }
        flyIntent[targetId] = enabled
        return true
    }

    fun onPlayerQuit(playerId: UUID) {
        if (settings().removeGodOnDisconnect
            && godPlayers.remove(playerId)
        ) {
            backend.setInvulnerable(playerId, false)
        }
        flyIntent.remove(playerId)
        lastHeal.remove(playerId)
        lastWorld.remove(playerId)
        lastGameMode.remove(playerId)
    }

    fun clearAll() {
        godPlayers.clear()
        flyIntent.clear()
        lastHeal.clear()
        lastWorld.clear()
        lastGameMode.clear()
    }

    /**
     * Tracks dimension and game-mode transitions for one online player. Re-applies the god/fly
     * world policy on a dimension change and restores an operator-set flight state across a
     * game-mode change when `preserveFlyOnGamemodeChange` is enabled.
     */
    fun onPlayerObserved(playerId: UUID) {
        val config = settings()
        val dimension = backend.dimension(playerId)
            ?: return
        val previousWorld = lastWorld.put(playerId, dimension)
        if (previousWorld != null && previousWorld != dimension) {
            if (config.resetFlyOnWorldChange
                && backend.mayFly(playerId)
            ) {
                backend.setFly(playerId, false)
                flyIntent[playerId] = false
            }
            if (playerId in godPlayers
                && isNoGodWorld(dimension)
            ) {
                if (backend.setInvulnerable(playerId, false)) {
                    godPlayers.remove(playerId)
                }
            }
        }

        val gameMode = backend.gameMode(playerId) ?: return
        val previousMode = lastGameMode.put(playerId, gameMode)
        if (previousMode != null
            && previousMode != gameMode
            && config.preserveFlyOnGamemodeChange
            && flyIntent[playerId] == true
        ) {
            backend.setFly(playerId, true)
        }
    }

    private fun isNoGodWorld(dimension: String?): Boolean =
        dimension != null
                && settings().noGodWorlds.contains(dimension)

}
