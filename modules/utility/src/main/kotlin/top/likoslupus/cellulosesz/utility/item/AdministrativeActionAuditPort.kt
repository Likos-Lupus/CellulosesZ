package top.likoslupus.cellulosesz.utility.item

import java.util.*

/**
 * Cross-module audit seam. Utility reports an administrative action it already applied (for
 * example clearing another player's inventory) so administration can record it, without utility
 * depending on administration. The game action already happened, so the port is best-effort.
 */
public fun interface AdministrativeActionAuditPort {

    public suspend fun record(
        actorId: UUID,
        targetId: UUID,
        targetName: String,
        cleared: Boolean,
    )

}
