package top.likoslupus.cellulosesz.movement.request

import java.time.Instant
import java.util.*
import kotlin.time.Duration

internal sealed interface TpaSendResult {

    data object Sent : TpaSendResult

    data object Self : TpaSendResult

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
 * In-memory teleport request state. Confined to the server thread: all command handlers and
 * disconnect events run there, so no additional locking is required.
 *
 * Policy: a sender has at most one outstanding outbound request; a new [send] replaces it.
 * Expiry is lazy, evaluated whenever the state is read.
 */
internal class TeleportRequestService(
    private val timeout: () -> Duration,
    private val clock: () -> Instant = Instant::now,
) {

    private val outbound = HashMap<UUID, TeleportRequest>()

    fun send(senderId: UUID, targetId: UUID): TpaSendResult {
        if (senderId == targetId) {
            return TpaSendResult.Self
        }

        val now = clock()
        purgeExpired(now)
        outbound[senderId] = TeleportRequest(
            senderId = senderId,
            targetId = targetId,
            createdAt = now,
            expiresAt = now.plusNanos(timeout().inWholeNanoseconds),
        )
        return TpaSendResult.Sent
    }

    fun accept(targetId: UUID, senderId: UUID?): TpaAcceptResult {
        purgeExpired(clock())
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
        purgeExpired(clock())
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
        purgeExpired(clock())
        return outbound.remove(senderId)
    }

    /**
     * Drops requests sent by [playerId] and requests targeting [playerId].
     * Returns the senders whose outbound request was dropped because the target left.
     */
    fun clearPlayer(playerId: UUID): List<UUID> {
        purgeExpired(clock())
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

    private fun purgeExpired(now: Instant) {
        outbound.entries.removeIf { it.value.isExpired(now) }
    }

}
