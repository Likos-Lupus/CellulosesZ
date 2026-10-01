package top.likoslupus.cellulosesz.movement.request

import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal sealed interface TpaSendResult {

    data class Sent(val targetId: UUID) : TpaSendResult

    data class Refreshed(val targetId: UUID) : TpaSendResult

    data object Self : TpaSendResult

    data object SenderAlreadyHasRequest : TpaSendResult

    data object TargetQueueFull : TpaSendResult

}

internal sealed interface TpaAcceptResult {

    data class Accepted(val request: TeleportRequest) : TpaAcceptResult

    data object None : TpaAcceptResult

    data object Ambiguous : TpaAcceptResult

}

internal sealed interface TpaDenyResult {

    data class Denied(val request: TeleportRequest) : TpaDenyResult

    data object None : TpaDenyResult

    data object Ambiguous : TpaDenyResult

}

/**
 * In-memory teleport-request state. Confined to the server thread: sending, accepting, denying, and
 * disconnect events all run there, so plain maps are used with no locking. Expiry is lazy.
 *
 * Policy: a sender has at most one outbound request (a same-target, same-type send refreshes it);
 * a target accepts at most [TeleportRequestSettings.maxIncomingPerPlayer] concurrent requests.
 */
internal class TeleportRequestService(
    private val settings: () -> TeleportRequestSettings,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    private val outbound = HashMap<UUID, TeleportRequest>()

    fun send(
        senderId: UUID,
        targetId: UUID,
        type: TeleportRequestType
    ): TpaSendResult {
        if (senderId == targetId) {
            return TpaSendResult.Self
        }

        purgeExpired()

        val existing = outbound[senderId]
        if (existing != null) {
            if (existing.targetId != targetId || existing.type != type) {
                return TpaSendResult.SenderAlreadyHasRequest
            }

            outbound[senderId] = existing.copy(createdAt = timeSource.markNow())
            return TpaSendResult.Refreshed(targetId)
        }

        if (incomingCount(targetId) >= settings().maxIncomingPerPlayer) {
            return TpaSendResult.TargetQueueFull
        }

        outbound[senderId] = TeleportRequest(
            senderId,
            targetId,
            type,
            timeSource.markNow()
        )
        return TpaSendResult.Sent(targetId)
    }

    fun accept(targetId: UUID, senderId: UUID?): TpaAcceptResult {
        purgeExpired()
        val candidates = outbound.values.filter { it.targetId == targetId }
        val request = when {
            senderId != null ->
                candidates.firstOrNull { it.senderId == senderId }

            candidates.size == 1 ->
                candidates.single()

            candidates.size > 1 ->
                return TpaAcceptResult.Ambiguous

            else -> null
        } ?: return TpaAcceptResult.None

        outbound.remove(request.senderId)
        return TpaAcceptResult.Accepted(request)
    }

    fun deny(targetId: UUID, senderId: UUID?): TpaDenyResult {
        purgeExpired()
        val candidates = outbound.values.filter { it.targetId == targetId }
        val request = when {
            senderId != null ->
                candidates.firstOrNull { it.senderId == senderId }

            candidates.size == 1 ->
                candidates.single()

            candidates.size > 1 ->
                return TpaDenyResult.Ambiguous

            else -> null
        } ?: return TpaDenyResult.None

        outbound.remove(request.senderId)
        return TpaDenyResult.Denied(request)
    }

    fun cancel(senderId: UUID): TeleportRequest? {
        purgeExpired()
        return outbound.remove(senderId)
    }

    /**
     * Drops requests sent by [playerId] and requests targeting [playerId].
     * Returns the senders whose outbound request was dropped because the target left.
     */
    fun clearPlayer(playerId: UUID): List<UUID> {
        purgeExpired()
        val affectedSenders = mutableListOf<UUID>()
        outbound.remove(playerId)
        val iterator = outbound.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.targetId == playerId) {
                affectedSenders.add(entry.key)
                iterator.remove()
            }
        }
        return affectedSenders
    }

    private fun incomingCount(targetId: UUID): Int =
        outbound.values.count { it.targetId == targetId }

    private fun purgeExpired() {
        val timeout = settings().timeoutSeconds.seconds
        outbound.values.removeIf { it.isExpired(timeout) }
    }

}
