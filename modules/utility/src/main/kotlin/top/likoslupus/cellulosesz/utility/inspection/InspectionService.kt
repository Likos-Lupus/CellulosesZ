package top.likoslupus.cellulosesz.utility.inspection

import java.util.*

internal class InspectionService(private val backend: InspectionBackend) {

    fun enderChest(playerId: UUID): InspectionOpenResult =
        backend.openEnderChest(playerId)

    fun inspect(viewerId: UUID, targetId: UUID): InspectionOpenResult =
        backend.openInventoryView(viewerId, targetId)

    fun disposal(playerId: UUID): InspectionOpenResult =
        backend.openDisposal(playerId)

}
