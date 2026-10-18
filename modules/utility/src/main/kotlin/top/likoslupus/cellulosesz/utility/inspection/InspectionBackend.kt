package top.likoslupus.cellulosesz.utility.inspection

import java.util.*

internal sealed interface InspectionOpenResult {

    data object Opened : InspectionOpenResult
    data object PlayerOffline : InspectionOpenResult
    data object Failed : InspectionOpenResult

}

/**
 * Inventory inspection helpers. Only vanilla menu types are used. `/enderchest` and an editable
 * `/invsee` back the menu with the target's real container (write-through); a non-editable `/invsee`
 * snapshots into a read-only container. Implementations are server-thread confined.
 */
internal interface InspectionBackend {

    fun openEnderChest(viewerId: UUID, targetId: UUID): InspectionOpenResult

    fun openInventoryView(
        viewerId: UUID,
        targetId: UUID,
        editable: Boolean,
        equip: Boolean,
    ): InspectionOpenResult

    fun openDisposal(playerId: UUID): InspectionOpenResult

}
