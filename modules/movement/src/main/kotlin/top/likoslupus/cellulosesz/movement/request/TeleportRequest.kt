package top.likoslupus.cellulosesz.movement.request

import java.util.*
import kotlin.time.Duration
import kotlin.time.TimeMark

/** Transient teleport request. Time is a [TimeMark] so expiry is elapsed duration, not a calendar. */
internal data class TeleportRequest(
    val senderId: UUID,
    val targetId: UUID,
    val type: TeleportRequestType,
    val createdAt: TimeMark,
) {

    fun isExpired(timeout: Duration): Boolean =
        createdAt.elapsedNow() >= timeout

}
