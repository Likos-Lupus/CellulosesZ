package top.likoslupus.cellulosesz.utility.inspection

import java.util.*

internal sealed interface InspectionOpenResult {

    data object Opened : InspectionOpenResult
    data object PlayerOffline : InspectionOpenResult
    data object Failed : InspectionOpenResult

}

/**
 * Inventory inspection helpers. Only vanilla menu types are used; `/invsee` is strictly read-only.
 * Implementations are server-thread confined.
 */
internal interface InspectionBackend {

    fun openEnderChest(playerId: UUID): InspectionOpenResult

    fun openInventoryView(viewerId: UUID, targetId: UUID): InspectionOpenResult

    fun openDisposal(playerId: UUID): InspectionOpenResult

}
