package top.likoslupus.cellulosesz.movement.teleport

import java.util.*

/**
 * The single cross-world access gate. [TeleportCoordinator] consults it for every intent whose
 * resolved destination is in a different dimension than the subject's current one; the Minecraft
 * implementation resolves the subject and evaluates `cellulosesz.world.<dimension>`.
 */
internal fun interface TeleportWorldAccess {

    suspend fun allows(subjectId: UUID, destinationDimension: String): Boolean

    companion object {

        val AllowAll: TeleportWorldAccess = TeleportWorldAccess { _, _ -> true }

    }

}
