package top.likoslupus.cellulosesz.movement.request

import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings
import java.util.*

/** How an owner auto-accepts incoming teleport requests. */
internal enum class TpAutoMode(val id: String) {

    OFF("off"),
    TRUSTED("trusted"),
    ALL("all");

    companion object {

        fun parse(raw: String): TpAutoMode? =
            entries.firstOrNull { it.id.equals(raw, ignoreCase = true) }

    }

}

internal sealed interface TpAutoToggleResult {

    data class Updated(val mode: TpAutoMode) : TpAutoToggleResult

    data object StorageUnavailable : TpAutoToggleResult

}

internal sealed interface TpAutoAllowResult {

    data object Added : TpAutoAllowResult

    data object AlreadyAllowed : TpAutoAllowResult

    data object Self : TpAutoAllowResult

    data object LimitReached : TpAutoAllowResult

    data object StorageUnavailable : TpAutoAllowResult

}

internal sealed interface TpAutoDisallowResult {

    data object Removed : TpAutoDisallowResult

    data object NotAllowed : TpAutoDisallowResult

    data object StorageUnavailable : TpAutoDisallowResult

}

/**
 * Server-thread confined auto-accept preferences. Kept in memory so the request hot path never
 * touches storage: [shouldAutoAccept] is a pure map lookup. Mutations write first, then publish.
 *
 * Auto-accept applies only to [TeleportRequestType.TO_TARGET] requests (the requester moves to the
 * owner); [TeleportRequestType.BRING_TARGET] is never auto-accepted, because that would move the
 * owner to the requester. [TpAutoMode.TRUSTED] accepts only allow-listed senders; [TpAutoMode.ALL]
 * accepts every eligible sender. Ignore and visibility rules are applied by the request path.
 */
internal class TpAutoService(
    private val repository: TpAutoRepository,
    private val settings: () -> TeleportRequestSettings,
) {

    private val modes = HashMap<UUID, TpAutoMode>()
    private val allowed = HashMap<UUID, MutableSet<UUID>>()

    /** Blocking startup load; must run on the server-thread bootstrap before players can act. */
    fun hydrate() {
        val snapshot = repository.loadAllBlocking()
        modes.clear()
        modes.putAll(snapshot.modes)
        allowed.clear()
        snapshot.allowed.forEach { (owner, members) ->
            if (members.isNotEmpty()) {
                allowed[owner] = members.toMutableSet()
            }
        }
    }

    fun mode(ownerId: UUID): TpAutoMode =
        modes[ownerId] ?: TpAutoMode.OFF

    fun allowedBy(ownerId: UUID): List<UUID> =
        allowed[ownerId]?.sorted().orEmpty()

    /** Pure, in-memory decision used by the request path. */
    fun shouldAutoAccept(
        ownerId: UUID,
        senderId: UUID,
        type: TeleportRequestType,
    ): Boolean {
        return !(type != TeleportRequestType.TO_TARGET || ownerId == senderId) && when (mode(ownerId)) {
            TpAutoMode.OFF -> false
            TpAutoMode.TRUSTED -> allowed[ownerId]?.contains(senderId) == true
            TpAutoMode.ALL -> true
        }
    }

    suspend fun setMode(ownerId: UUID, value: TpAutoMode): TpAutoToggleResult =
        try {
            repository.setMode(ownerId, value)
            if (value == TpAutoMode.OFF) {
                modes.remove(ownerId)
            } else {
                modes[ownerId] = value
            }
            TpAutoToggleResult.Updated(value)
        } catch (_: TpAutoDataException) {
            TpAutoToggleResult.StorageUnavailable
        }

    suspend fun allow(ownerId: UUID, targetId: UUID): TpAutoAllowResult {
        if (ownerId == targetId) {
            return TpAutoAllowResult.Self
        }
        val members = allowed[ownerId]
        if (members != null && targetId in members) {
            return TpAutoAllowResult.AlreadyAllowed
        }
        if ((members?.size ?: 0) >= settings().maxAutoAllowedPlayers) {
            return TpAutoAllowResult.LimitReached
        }
        return try {
            repository.addAllowed(ownerId, targetId)
            allowed.getOrPut(ownerId) { mutableSetOf() }.add(targetId)
            TpAutoAllowResult.Added
        } catch (_: TpAutoDataException) {
            TpAutoAllowResult.StorageUnavailable
        }
    }

    suspend fun disallow(ownerId: UUID, targetId: UUID): TpAutoDisallowResult {
        val members = allowed[ownerId]
        if (members == null || targetId !in members) {
            return TpAutoDisallowResult.NotAllowed
        }
        return try {
            repository.removeAllowed(ownerId, targetId)
            members.remove(targetId)
            if (members.isEmpty()) {
                allowed.remove(ownerId)
            }
            TpAutoDisallowResult.Removed
        } catch (_: TpAutoDataException) {
            TpAutoDisallowResult.StorageUnavailable
        }
    }

}
