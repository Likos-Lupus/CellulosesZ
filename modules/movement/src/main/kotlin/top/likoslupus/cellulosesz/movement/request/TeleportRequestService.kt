package top.likoslupus.cellulosesz.movement.request

import top.likoslupus.cellulosesz.movement.config.TeleportRequestSettings
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal sealed interface TpaSendResult {

    data class Sent(val targetId: UUID) : TpaSendResult

    data class Refreshed(val targetId: UUID) : TpaSendResult

    data object Self : TpaSendResult

    data object SenderQueueFull : TpaSendResult

    data object ServerQueueFull : TpaSendResult

    data class TargetQueueFull(val targetId: UUID) : TpaSendResult

}

internal sealed interface TpaBatchSendResult {

    data class Sent(val recipients: List<UUID>) : TpaBatchSendResult

    data object NoRecipients : TpaBatchSendResult

    data class Cooldown(val remaining: Duration) : TpaBatchSendResult

    data object SenderQueueFull : TpaBatchSendResult

    data object ServerQueueFull : TpaBatchSendResult

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
 * Policy: a sender keeps an outbound index keyed by target, so one sender may hold simultaneous
 * requests to distinct targets (bounded by [TeleportRequestSettings.maxOutgoingPerPlayer] and the
 * server-wide [TeleportRequestSettings.maxRequestsServerWide]); a target accepts at most
 * [TeleportRequestSettings.maxIncomingPerPlayer] concurrent requests. A same-target send refreshes
 * the recorded location and direction.
 */
internal class TeleportRequestService(
    private val settings: () -> TeleportRequestSettings,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    private val outbound = HashMap<UUID, MutableMap<UUID, TeleportRequest>>()
    private val batchMarks = HashMap<UUID, TimeMark>()

    fun send(
        senderId: UUID,
        targetId: UUID,
        type: TeleportRequestType,
    ): TpaSendResult {
        if (senderId == targetId) {
            return TpaSendResult.Self
        }

        purgeExpired()

        val byTarget = outbound[senderId]
        val existing = byTarget?.get(targetId)
        return when {
            existing != null -> {
                byTarget[targetId] = existing.copy(
                    type = type,
                    createdAt = timeSource.markNow()
                )
                TpaSendResult.Refreshed(targetId)
            }

            totalCount() >= settings().maxRequestsServerWide ->
                TpaSendResult.ServerQueueFull

            (byTarget?.size ?: 0) >= settings().maxOutgoingPerPlayer ->
                TpaSendResult.SenderQueueFull

            incomingCount(targetId) >= settings().maxIncomingPerPlayer ->
                TpaSendResult.TargetQueueFull(targetId)

            else -> {
                val target = byTarget
                    ?: HashMap<UUID, TeleportRequest>().also { outbound[senderId] = it }
                target[targetId] = TeleportRequest(
                    senderId,
                    targetId,
                    type,
                    timeSource.markNow()
                )
                TpaSendResult.Sent(targetId)
            }
        }
    }

    /**
     * Sends one request to each reachable candidate, honoring the batch cap, the per-sender outbound
     * cap, the server-wide cap, and the batch cooldown. [reachable] is evaluated per candidate so the
     * caller's reachability policy filters recipients without leaking into this class.
     */
    fun sendBatch(
        senderId: UUID,
        candidates: List<UUID>,
        type: TeleportRequestType,
        reachable: (UUID) -> Boolean,
    ): TpaBatchSendResult {
        purgeExpired()

        remainingBatchCooldown(senderId)?.let {
            return TpaBatchSendResult.Cooldown(it)
        }

        val eligible = candidates.asSequence()
                .filter { it != senderId }
                .filter(reachable)
                .distinct()
                .take(settings().maxBatchRecipients)
                .toList()
        if (eligible.isEmpty()) {
            return TpaBatchSendResult.NoRecipients
        }

        val recipients = mutableListOf<UUID>()
        var blocking: TpaBatchSendResult? = null
        for (targetId in eligible) {
            when (send(senderId, targetId, type)) {
                is TpaSendResult.Sent,
                is TpaSendResult.Refreshed ->
                    recipients += targetId

                TpaSendResult.SenderQueueFull -> {
                    blocking = TpaBatchSendResult.SenderQueueFull
                    break
                }

                TpaSendResult.ServerQueueFull -> {
                    blocking = TpaBatchSendResult.ServerQueueFull
                    break
                }

                else -> Unit
            }
        }

        return when {
            recipients.isNotEmpty() -> {
                batchMarks[senderId] = timeSource.markNow()
                TpaBatchSendResult.Sent(recipients)
            }

            blocking != null -> blocking
            else -> TpaBatchSendResult.NoRecipients
        }
    }

    fun accept(targetId: UUID, senderId: UUID?): TpaAcceptResult {
        purgeExpired()
        val candidates = requestsFor(targetId)
        val request = when {
            senderId != null ->
                candidates.firstOrNull { it.senderId == senderId }

            candidates.size == 1 ->
                candidates.single()

            candidates.size > 1 ->
                return TpaAcceptResult.Ambiguous

            else -> null
        } ?: return TpaAcceptResult.None

        remove(request.senderId, request.targetId)
        return TpaAcceptResult.Accepted(request)
    }

    fun deny(targetId: UUID, senderId: UUID?): TpaDenyResult {
        purgeExpired()
        val candidates = requestsFor(targetId)
        val request = when {
            senderId != null ->
                candidates.firstOrNull { it.senderId == senderId }

            candidates.size == 1 ->
                candidates.single()

            candidates.size > 1 ->
                return TpaDenyResult.Ambiguous

            else -> null
        } ?: return TpaDenyResult.None

        remove(request.senderId, request.targetId)
        return TpaDenyResult.Denied(request)
    }

    /** Drops and returns every outbound request held by [senderId]. */
    fun cancel(senderId: UUID): List<TeleportRequest> {
        purgeExpired()
        return outbound.remove(senderId)?.values?.toList().orEmpty()
    }

    /**
     * Drops the single outbound request from [senderId] to [targetId], returning whether it existed.
     * Used by the public API so cancelling one request does not drop a sender's other requests.
     */
    fun cancelOne(senderId: UUID, targetId: UUID): Boolean {
        purgeExpired()
        val byTarget = outbound[senderId]
            ?: return false
        if (byTarget.remove(targetId) == null) {
            return false
        }
        if (byTarget.isEmpty()) {
            outbound.remove(senderId)
        }
        return true
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
            if (entry.value.values.removeIf { it.targetId == playerId }) {
                affectedSenders.add(entry.key)
            }
            if (entry.value.isEmpty()) {
                iterator.remove()
            }
        }
        return affectedSenders
    }

    private fun remove(senderId: UUID, targetId: UUID) {
        val byTarget = outbound[senderId] ?: return
        byTarget.remove(targetId)
        if (byTarget.isEmpty()) {
            outbound.remove(senderId)
        }
    }

    private fun requestsFor(targetId: UUID): List<TeleportRequest> =
        outbound.values.flatMap { requests ->
            requests.values.filter { it.targetId == targetId }
        }

    private fun totalCount(): Int =
        outbound.values.sumOf { it.size }

    private fun incomingCount(targetId: UUID): Int =
        outbound.values.count { it.containsKey(targetId) }

    private fun remainingBatchCooldown(senderId: UUID): Duration? {
        val cooldown = settings().batchCooldownSeconds.seconds
        if (cooldown <= Duration.ZERO) {
            return null
        }

        val mark = batchMarks[senderId] ?: return null
        val left = cooldown - mark.elapsedNow()
        return left.takeIf { it > Duration.ZERO }
    }

    private fun purgeExpired() {
        val timeout = settings().timeoutSeconds.seconds
        val iterator = outbound.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.values.removeIf { it.isExpired(timeout) }
            if (entry.value.isEmpty()) {
                iterator.remove()
            }
        }
    }

}
