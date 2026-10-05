package top.likoslupus.cellulosesz.utility.item

import java.util.*

/** Which held/equipped items `/repair` acts on. */
internal enum class RepairScope {

    HAND,
    ALL,

}

internal sealed interface RepairBackendResult {

    data class Repaired(
        val count: Int,
        val skippedEnchanted: Int
    ) : RepairBackendResult

    data object TargetOffline : RepairBackendResult

    data object NothingToRepair : RepairBackendResult

}

internal sealed interface MoreBackendResult {

    data class Filled(val itemName: String) : MoreBackendResult

    data object TargetOffline : MoreBackendResult

    data object NothingToFill : MoreBackendResult

}

internal sealed interface CondenseBackendResult {

    data class Condensed(val conversions: Int) : CondenseBackendResult

    data object TargetOffline : CondenseBackendResult

    data object NothingToCondense : CondenseBackendResult

}

/**
 * All item/inventory mutation for the item utility features. Implementations are server-thread
 * confined; the service hops onto the server thread through its runner before calling them.
 */
internal interface ItemUtilityBackend {

    fun repair(
        playerId: UUID,
        scope: RepairScope,
        allowEnchanted: Boolean,
        includeArmor: Boolean,
    ): RepairBackendResult

    fun more(
        playerId: UUID,
        amount: Int?,
    ): MoreBackendResult

    fun condense(playerId: UUID): CondenseBackendResult

}
