package top.likoslupus.cellulosesz.tpa

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
