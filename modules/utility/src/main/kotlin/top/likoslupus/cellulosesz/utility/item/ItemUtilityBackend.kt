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

internal sealed interface ItemNameBackendResult {

    /** The custom name was set; [displayName] is the resulting item display name. */
    data class Set(val displayName: String) : ItemNameBackendResult

    /** The custom name was cleared. */
    data class Cleared(val displayName: String) : ItemNameBackendResult

    data object TargetOffline : ItemNameBackendResult

    data object NoHeldItem : ItemNameBackendResult

}

internal sealed interface ItemLoreBackendResult {

    data class Set(val lineCount: Int) : ItemLoreBackendResult

    data class Cleared(val lineCount: Int) : ItemLoreBackendResult

    data object TargetOffline : ItemLoreBackendResult

    data object NoHeldItem : ItemLoreBackendResult

}

internal sealed interface HatBackendResult {

    data class Worn(val itemName: String) : HatBackendResult

    data object TargetOffline : HatBackendResult

    data object NoHeldItem : HatBackendResult

    /** The held item has no equipment capability and `allowAnyItemAsHat` is false. */
    data object NotAllowed : HatBackendResult

}

internal sealed interface ClearInventoryBackendResult {

    data class Cleared(val stacks: Int) : ClearInventoryBackendResult

    data object TargetOffline : ClearInventoryBackendResult

}

/** Which slots a `/clearinventory` operation touches. */
internal enum class ClearScope {

    /** Main inventory and hotbar. */
    MAIN,

    /** Main inventory, hotbar, armor and offhand. Ender chest and other inventories are never touched. */
    ALL,

}

/**
 * One slot's identity plus a fingerprint of the full stack (item, components and count). Empty slots
 * still contribute an entry so that filling an empty slot is detected as a change.
 */
internal data class SlotFingerprint(
    val slot: String,
    val digest: String,
)

/** An immutable snapshot of the slots a [ClearScope] covers, used to detect any change before apply. */
internal data class InventorySnapshot(
    val scope: ClearScope,
    val stacks: Int,
    val fingerprints: List<SlotFingerprint>,
)

internal sealed interface InventorySnapshotResult {

    data class Snapshot(val value: InventorySnapshot) : InventorySnapshotResult

    data object TargetOffline : InventorySnapshotResult

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

    /** Sets the held item's custom name, or clears it when [name] is null. */
    fun setItemName(playerId: UUID, name: String?): ItemNameBackendResult

    /** Replaces the held item's lore. An empty [lines] clears it. */
    fun setItemLore(playerId: UUID, lines: List<String>): ItemLoreBackendResult

    /** Swaps the held item onto the head slot. */
    fun wearHeldItem(playerId: UUID, allowAnyItem: Boolean): HatBackendResult

    /** Fingerprints the slots covered by [scope] for the confirmation transaction. */
    fun snapshotInventory(playerId: UUID, scope: ClearScope): InventorySnapshotResult

    /** Clears the slots covered by [scope]. */
    fun clearInventory(playerId: UUID, scope: ClearScope): ClearInventoryBackendResult

}
