package top.likoslupus.cellulosesz.movement.request

import java.time.Instant
import java.util.*

internal data class TeleportRequest(
    val senderId: UUID,
    val targetId: UUID,
    val createdAt: Instant,
    val expiresAt: Instant,
) {

    fun isExpired(now: Instant): Boolean =
        !now.isBefore(expiresAt)

}
