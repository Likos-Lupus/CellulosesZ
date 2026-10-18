package top.likoslupus.cellulosesz.utility.inspection

import java.util.*

internal class InspectionService(private val backend: InspectionBackend) {

    fun enderChest(viewerId: UUID, targetId: UUID): InspectionOpenResult =
        backend.openEnderChest(viewerId, targetId)

    fun inspect(
        viewerId: UUID,
        targetId: UUID,
        editable: Boolean,
        equip: Boolean,
    ): InspectionOpenResult =
        backend.openInventoryView(viewerId, targetId, editable, equip)

    fun disposal(playerId: UUID): InspectionOpenResult =
        backend.openDisposal(playerId)

}
