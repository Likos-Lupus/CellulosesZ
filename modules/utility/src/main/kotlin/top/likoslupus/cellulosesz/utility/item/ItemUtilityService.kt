package top.likoslupus.cellulosesz.utility.item

import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.utility.config.ItemEditingSettings
import top.likoslupus.cellulosesz.utility.config.ItemUtilitySettings
import java.time.Instant
import java.util.*
import kotlin.random.Random

internal sealed interface RepairResult {

    data class Repaired(
        val playerName: String,
        val count: Int,
        val skippedEnchanted: Int,
    ) : RepairResult

    data object Disabled : RepairResult

    data object TargetOffline : RepairResult

    data object NothingToRepair : RepairResult

}

internal sealed interface MoreResult {

    data class Filled(
        val playerName: String,
        val itemName: String
    ) : MoreResult

    data object Disabled : MoreResult

    data object TargetOffline : MoreResult

    data object NothingToFill : MoreResult

}

internal sealed interface CondenseResult {

    data class Condensed(
        val playerName: String,
        val conversions: Int
    ) : CondenseResult

    data object Disabled : CondenseResult

    data object TargetOffline : CondenseResult

    data object NothingToCondense : CondenseResult

}

internal sealed interface ItemNameResult {

    data class Set(
        val playerName: String,
        val displayName: String,
    ) : ItemNameResult

    data class Cleared(
        val playerName: String,
        val displayName: String,
    ) : ItemNameResult

    data class TooLong(val limit: Int) : ItemNameResult

    data object TargetOffline : ItemNameResult

    data object NoHeldItem : ItemNameResult

}

internal sealed interface ItemLoreResult {

    data class Set(
        val playerName: String,
        val lines: Int,
    ) : ItemLoreResult

    data class Cleared(val playerName: String) : ItemLoreResult

    data class TooManyLines(val limit: Int) : ItemLoreResult

    data class LineTooLong(val limit: Int) : ItemLoreResult

    data object TargetOffline : ItemLoreResult

    data object NoHeldItem : ItemLoreResult

}

internal sealed interface HatResult {

    data class Worn(
        val playerName: String,
        val itemName: String,
    ) : HatResult

    data object NotAllowed : HatResult

    data object TargetOffline : HatResult

    data object NoHeldItem : HatResult

}

internal sealed interface ClearRequestResult {

    data class Issued(
        val code: String,
        val seconds: Int,
        val scope: ClearScope,
        val stacks: Int,
        val targetName: String?,
        val targetId: UUID?,
    ) : ClearRequestResult

    data object TargetOffline : ClearRequestResult

}

internal sealed interface ClearInventoryResult {

    data class Cleared(
        val playerName: String,
        val stacks: Int,
        val targetName: String?,
    ) : ClearInventoryResult

    data object InvalidCode : ClearInventoryResult

    data object TargetOffline : ClearInventoryResult

    /** The inventory changed between preview and confirm, so the transaction was cancelled. */
    data object InventoryChanged : ClearInventoryResult

}

/**
 * Item utility orchestration. These are synchronous because command handlers already run on the
 * server thread and the work is immediate (no filesystem IO).
 */
internal class ItemUtilityService(
    private val backend: ItemUtilityBackend,
    private val known: KnownPlayerResolver,
    private val settings: () -> ItemUtilitySettings,
    private val editing: () -> ItemEditingSettings = { ItemEditingSettings() },
    clock: () -> Instant = { Instant.now() },
    confirmationCode: () -> String = {
        Random.nextInt(100_000, 1_000_000).toString()
    },
) {

    private val confirmations = ClearInventoryConfirmations(
        windowSeconds = { editing().clearConfirmationSeconds },
        clock = clock,
        codeGenerator = confirmationCode,
    )

    fun repair(targetId: UUID, scope: RepairScope): RepairResult {
        val config = settings()
        if (!config.repairEnabled) {
            return RepairResult.Disabled
        }

        val name = known.onlineById(targetId)?.name
            ?: return RepairResult.TargetOffline

        return when (
            val result = backend.repair(
                targetId,
                scope,
                config.repairEnchanted,
                config.repairAllIncludesArmor,
            )
        ) {
            is RepairBackendResult.Repaired -> RepairResult.Repaired(
                name,
                result.count,
                result.skippedEnchanted
            )

            RepairBackendResult.TargetOffline -> RepairResult.TargetOffline
            RepairBackendResult.NothingToRepair -> RepairResult.NothingToRepair
        }
    }

    fun more(targetId: UUID, amount: Int?): MoreResult {
        if (!settings().moreEnabled) {
            return MoreResult.Disabled
        }
        val name = known.onlineById(targetId)?.name
            ?: return MoreResult.TargetOffline

        return when (val result = backend.more(targetId, amount)) {
            is MoreBackendResult.Filled -> MoreResult.Filled(
                name,
                result.itemName
            )

            MoreBackendResult.TargetOffline -> MoreResult.TargetOffline
            MoreBackendResult.NothingToFill -> MoreResult.NothingToFill
        }
    }

    fun condense(targetId: UUID): CondenseResult {
        if (!settings().condenseEnabled) {
            return CondenseResult.Disabled
        }
        val name = known.onlineById(targetId)?.name
            ?: return CondenseResult.TargetOffline

        return when (val result = backend.condense(targetId)) {
            is CondenseBackendResult.Condensed -> CondenseResult.Condensed(
                name,
                result.conversions
            )

            CondenseBackendResult.TargetOffline -> CondenseResult.TargetOffline
            CondenseBackendResult.NothingToCondense -> CondenseResult.NothingToCondense
        }
    }

    fun setItemName(targetId: UUID, requested: String?): ItemNameResult {
        val playerName = known.onlineById(targetId)?.name
            ?: return ItemNameResult.TargetOffline
        val resolved: String? = when (requested) {
            null -> null
            else -> when (
                val validation = ItemEditingRules.validateName(
                    requested,
                    editing().maxItemNameLength,
                )
            ) {
                is NameValidation.Valid -> validation.name
                is NameValidation.TooLong -> return ItemNameResult.TooLong(validation.limit)
            }
        }

        return when (val result = backend.setItemName(targetId, resolved)) {
            is ItemNameBackendResult.Set ->
                ItemNameResult.Set(playerName, result.displayName)

            is ItemNameBackendResult.Cleared ->
                ItemNameResult.Cleared(
                    playerName,
                    result.displayName
                )

            ItemNameBackendResult.TargetOffline ->
                ItemNameResult.TargetOffline

            ItemNameBackendResult.NoHeldItem ->
                ItemNameResult.NoHeldItem
        }
    }

    fun setItemLore(targetId: UUID, requested: String?): ItemLoreResult {
        val playerName = known.onlineById(targetId)?.name
            ?: return ItemLoreResult.TargetOffline
        val lines: List<String> = when (requested) {
            null -> emptyList()
            else -> when (
                val validation = ItemEditingRules.parseLore(
                    requested,
                    editing().maxLoreLines,
                    editing().maxLoreLineLength,
                )
            ) {
                is LoreValidation.Valid -> validation.lines
                is LoreValidation.TooManyLines -> return ItemLoreResult.TooManyLines(validation.limit)
                is LoreValidation.LineTooLong -> return ItemLoreResult.LineTooLong(validation.limit)
            }
        }

        return when (val result = backend.setItemLore(targetId, lines)) {
            is ItemLoreBackendResult.Set -> ItemLoreResult.Set(playerName, result.lineCount)
            is ItemLoreBackendResult.Cleared -> ItemLoreResult.Cleared(playerName)
            ItemLoreBackendResult.TargetOffline -> ItemLoreResult.TargetOffline
            ItemLoreBackendResult.NoHeldItem -> ItemLoreResult.NoHeldItem
        }
    }

    fun wearHeldItem(targetId: UUID): HatResult {
        val playerName = known.onlineById(targetId)?.name
            ?: return HatResult.TargetOffline
        return when (
            val result = backend.wearHeldItem(
                targetId,
                editing().allowAnyItemAsHat,
            )
        ) {
            is HatBackendResult.Worn -> HatResult.Worn(playerName, result.itemName)
            HatBackendResult.NotAllowed -> HatResult.NotAllowed
            HatBackendResult.TargetOffline -> HatResult.TargetOffline
            HatBackendResult.NoHeldItem -> HatResult.NoHeldItem
        }
    }

    fun requestInventoryClear(actorId: UUID, scope: ClearScope): ClearRequestResult {
        known.onlineById(actorId) ?: return ClearRequestResult.TargetOffline
        val snapshot = when (val result = backend.snapshotInventory(actorId, scope)) {
            is InventorySnapshotResult.Snapshot -> result.value
            InventorySnapshotResult.TargetOffline -> return ClearRequestResult.TargetOffline
        }
        val code = confirmations.issue(actorId, actorId, snapshot)
        return ClearRequestResult.Issued(
            code = code,
            seconds = editing().clearConfirmationSeconds,
            scope = scope,
            stacks = snapshot.stacks,
            targetName = null,
            targetId = null,
        )
    }

    fun confirmInventoryClear(actorId: UUID, code: String): ClearInventoryResult {
        val confirmation = when (val claim = confirmations.claim(actorId, code)) {
            is ClearClaim.Claimed -> claim.confirmation
            ClearClaim.Invalid -> return ClearInventoryResult.InvalidCode
        }
        if (confirmation.targetId != actorId) {
            return ClearInventoryResult.InvalidCode
        }
        return apply(confirmation, targetName = null)
    }

    fun requestInventoryClearFor(
        actorId: UUID,
        targetName: String,
        scope: ClearScope,
    ): ClearRequestResult {
        val target = known.onlineByName(targetName)
            ?: return ClearRequestResult.TargetOffline
        val snapshot = when (val result = backend.snapshotInventory(target.id, scope)) {
            is InventorySnapshotResult.Snapshot -> result.value
            InventorySnapshotResult.TargetOffline -> return ClearRequestResult.TargetOffline
        }
        val code = confirmations.issue(actorId, target.id, snapshot)
        return ClearRequestResult.Issued(
            code = code,
            seconds = editing().clearConfirmationSeconds,
            scope = scope,
            stacks = snapshot.stacks,
            targetName = target.name,
            targetId = target.id,
        )
    }

    fun confirmInventoryClearFor(
        actorId: UUID,
        targetName: String,
        code: String,
    ): ClearInventoryResult {
        val target = known.onlineByName(targetName)
            ?: return ClearInventoryResult.TargetOffline
        val confirmation = when (val claim = confirmations.claim(actorId, code)) {
            is ClearClaim.Claimed -> claim.confirmation
            ClearClaim.Invalid -> return ClearInventoryResult.InvalidCode
        }
        if (confirmation.targetId != target.id) {
            return ClearInventoryResult.InvalidCode
        }
        return apply(confirmation, target.name)
    }

    private fun apply(
        confirmation: ClearConfirmation,
        targetName: String?,
    ): ClearInventoryResult {
        val playerName = known.onlineById(confirmation.targetId)?.name
            ?: return ClearInventoryResult.TargetOffline
        val scope = confirmation.snapshot.scope
        val fresh = when (val result = backend.snapshotInventory(confirmation.targetId, scope)) {
            is InventorySnapshotResult.Snapshot -> result.value
            InventorySnapshotResult.TargetOffline -> return ClearInventoryResult.TargetOffline
        }
        if (fresh != confirmation.snapshot) {
            return ClearInventoryResult.InventoryChanged
        }
        return when (val result = backend.clearInventory(confirmation.targetId, scope)) {
            is ClearInventoryBackendResult.Cleared ->
                ClearInventoryResult.Cleared(playerName, result.stacks, targetName)

            ClearInventoryBackendResult.TargetOffline ->
                ClearInventoryResult.TargetOffline
        }
    }

    fun forgetInventoryClear(playerId: UUID) {
        confirmations.forget(playerId)
    }

    /**
     * Clears the actor's own inventory without the confirmation round-trip. The snapshot guard is
     * not used here: with confirmation off there is no preview to compare against.
     */
    fun clearInventoryNow(actorId: UUID, scope: ClearScope): ClearInventoryResult {
        val identity = known.onlineById(actorId)
            ?: return ClearInventoryResult.TargetOffline
        return when (val result = backend.clearInventory(actorId, scope)) {
            is ClearInventoryBackendResult.Cleared ->
                ClearInventoryResult.Cleared(identity.name, result.stacks, null)

            ClearInventoryBackendResult.TargetOffline ->
                ClearInventoryResult.TargetOffline
        }
    }

    /** Clears another player's inventory without the confirmation round-trip. */
    fun clearInventoryForNow(targetName: String, scope: ClearScope): ClearInventoryResult {
        val target = known.onlineByName(targetName)
            ?: return ClearInventoryResult.TargetOffline
        return when (val result = backend.clearInventory(target.id, scope)) {
            is ClearInventoryBackendResult.Cleared ->
                ClearInventoryResult.Cleared(
                    target.name,
                    result.stacks,
                    target.name
                )

            ClearInventoryBackendResult.TargetOffline ->
                ClearInventoryResult.TargetOffline
        }
    }

    fun clearConfirmations() {
        confirmations.clear()
    }

}
