package top.likoslupus.cellulosesz.utility.format

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.utility.inspection.InspectionOpenResult
import top.likoslupus.cellulosesz.utility.item.CondenseResult
import top.likoslupus.cellulosesz.utility.item.MoreResult
import top.likoslupus.cellulosesz.utility.item.RepairResult
import top.likoslupus.cellulosesz.utility.kit.*
import top.likoslupus.cellulosesz.utility.workstation.WorkstationResult
import java.time.Duration

/**
 * Owns every user-facing utility string. Deliberately not a keyed template registry: user text is
 * literal and this project has no localization framework.
 */
internal object UtilityMessages {

    fun prefixed(message: String): Component =
        Messages.prefixed(message)

    fun requiresPlayer(): Component =
        prefixed("this command requires a player")

    fun runtimeStopping(): Component =
        prefixed("runtime is shutting down")

    fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.seconds.coerceAtLeast(0)
        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (days == 0L && hours == 0L && minutes == 0L) append("${seconds}s")
        }.trim()
    }

    private fun reuseLabel(reuse: KitReusePolicy): String =
        when (reuse) {
            KitReusePolicy.Always -> "unlimited"
            KitReusePolicy.Once -> "one-time"
            is KitReusePolicy.Cooldown -> "every ${formatDuration(reuse.duration)}"
        }

    // Kits: claiming -------------------------------------------------------------------------

    fun claim(requestedName: String, result: ClaimKitResult): Component =
        prefixed(
            when (result) {
                is ClaimKitResult.Delivered -> buildString {
                    append("received kit '${result.kit.value}'")
                    if (result.droppedStacks > 0) {
                        append("; ${result.droppedStacks} stack(s) were dropped because your inventory was full")
                    }
                    if (!result.claimRecordedFully) {
                        append("; claim metadata could not be finalized, contact an administrator")
                    }
                }

                ClaimKitResult.Disabled -> "kits are disabled"
                ClaimKitResult.CatalogLoading -> "the kit catalog is still loading"
                ClaimKitResult.StorageUnavailable -> "kit storage is unavailable"
                ClaimKitResult.InvalidName -> "invalid kit name"
                ClaimKitResult.NotFound -> "kit '$requestedName' not found"
                ClaimKitResult.PlayerOffline -> "this command requires an online player"
                ClaimKitResult.InventoryFull -> "your inventory is too full to receive that kit"
                is ClaimKitResult.Cooldown -> "kit '$requestedName' is on cooldown for" +
                        formatDuration(result.remaining)

                ClaimKitResult.AlreadyUsed -> "kit '$requestedName' can only be claimed once"
                ClaimKitResult.DeliveryFailedAfterReservation -> "kit delivery failed after the claim was recorded; contact an administrator"
            }
        )

    // Kits: administration -------------------------------------------------------------------

    fun create(result: CreateKitResult): Component =
        prefixed(
            when (result) {
                is CreateKitResult.Created -> "kit '${result.definition.name.value}' created from your inventory"
                CreateKitResult.Disabled -> "kits are disabled"
                CreateKitResult.InvalidName -> "invalid kit name"
                CreateKitResult.AlreadyExists -> "that kit already exists; use /updatekit"
                CreateKitResult.InvalidCooldown -> "invalid cooldown duration"
                CreateKitResult.CooldownTooLong -> "that cooldown exceeds the configured maximum"
                CreateKitResult.EmptyInventory -> "your inventory (main slots) is empty"
                CreateKitResult.TooManyItems -> "that is more items than a kit may hold"
                CreateKitResult.KitLimitReached -> "the kit limit has been reached"
                CreateKitResult.PlayerOffline -> "this command requires an online player"
                CreateKitResult.StorageUnavailable -> "kit storage is unavailable"
            }
        )

    fun update(result: UpdateKitResult): Component =
        prefixed(
            when (result) {
                is UpdateKitResult.Updated -> "kit '${result.definition.name.value}' updated"
                UpdateKitResult.Disabled -> "kits are disabled"
                UpdateKitResult.InvalidName -> "invalid kit name"
                UpdateKitResult.NotFound -> "that kit does not exist; use /createkit"
                UpdateKitResult.InvalidCooldown -> "invalid cooldown duration"
                UpdateKitResult.CooldownTooLong -> "that cooldown exceeds the configured maximum"
                UpdateKitResult.EmptyInventory -> "your inventory (main slots) is empty"
                UpdateKitResult.TooManyItems -> "that is more items than a kit may hold"
                UpdateKitResult.PlayerOffline -> "this command requires an online player"
                UpdateKitResult.StorageUnavailable -> "kit storage is unavailable"
            }
        )

    fun delete(result: DeleteKitResult): Component =
        prefixed(
            when (result) {
                is DeleteKitResult.Deleted -> "kit '${result.name.value}' deleted"
                DeleteKitResult.Disabled -> "kits are disabled"
                DeleteKitResult.InvalidName -> "invalid kit name"
                DeleteKitResult.NotFound -> "that kit does not exist"
                DeleteKitResult.StorageUnavailable -> "kit storage is unavailable"
            }
        )

    fun list(result: ListKitsResult): Component =
        prefixed(
            when (result) {
                is ListKitsResult.Listed ->
                    when {
                        result.entries.isEmpty() -> "no kits defined"
                        else -> "kits: " + result.entries.joinToString(", ") { entry ->
                            "${entry.name.value} " +
                                    "(${reuseLabel(entry.reuse)}, " +
                                    "${availabilityLabel(entry)})"
                        }
                    }

                ListKitsResult.Loading -> "the kit catalog is still loading"
                ListKitsResult.StorageUnavailable -> "kit storage is unavailable"
            })

    private fun availabilityLabel(entry: KitListEntry): String =
        when (val availability = entry.availability) {
            KitAvailability.Available -> "available"
            KitAvailability.Used -> "already used"
            is KitAvailability.Cooldown -> "available in ${formatDuration(availability.remaining)}"
        }

    fun show(result: ShowKitResult): Component =
        prefixed(
            when (result) {
                is ShowKitResult.Shown -> return showKit(result.definition)
                ShowKitResult.Disabled -> "kits are disabled"
                ShowKitResult.Loading -> "the kit catalog is still loading"
                ShowKitResult.StorageUnavailable -> "kit storage is unavailable"
                ShowKitResult.InvalidName -> "invalid kit name"
                ShowKitResult.NotFound -> "that kit does not exist"
            }
        )

    private fun showKit(definition: KitDefinition): Component =
        buildString {
            append("kit '${definition.name.value}' (${reuseLabel(definition.reuse)}):")
            definition.items.forEach { append("\n  ${stackLine(it)}") }
        }.let(::prefixed)

    private fun stackLine(stack: ItemStack): String =
        "${stack.count}x ${stack.hoverName.string}"

    fun reset(result: ResetKitResult): Component =
        prefixed(
            when (result) {
                is ResetKitResult.Reset -> "kit '${result.name.value}' reset for ${result.target.name}"
                ResetKitResult.NothingToReset -> "there was nothing to reset for that kit"
                ResetKitResult.Disabled -> "kits are disabled"
                ResetKitResult.InvalidName -> "invalid kit name"
                ResetKitResult.NotFound -> "that kit does not exist"
                ResetKitResult.StorageUnavailable -> "kit storage is unavailable"
            }
        )

    // Items ----------------------------------------------------------------------------------

    fun repair(result: RepairResult): Component =
        prefixed(
            when (result) {
                is RepairResult.Repaired -> buildString {
                    append("repaired ${result.count} item(s) for ${result.playerName}")
                    if (result.skippedEnchanted > 0) {
                        append("; skipped ${result.skippedEnchanted} enchanted item(s)")
                    }
                }

                RepairResult.Disabled -> "item repair is disabled"
                RepairResult.TargetOffline -> "that player is not online"
                RepairResult.NothingToRepair -> "there was nothing to repair"
            }
        )

    fun more(result: MoreResult): Component =
        prefixed(
            when (result) {
                is MoreResult.Filled -> "filled the held ${result.itemName} for ${result.playerName}"
                MoreResult.Disabled -> "that item utility is disabled"
                MoreResult.TargetOffline -> "that player is not online"
                MoreResult.NothingToFill -> "the held stack cannot be filled further"
            }
        )

    fun condense(result: CondenseResult): Component =
        prefixed(
            when (result) {
                is CondenseResult.Condensed -> "condensed ${result.conversions} stack(s) into compact blocks"
                CondenseResult.Disabled -> "condensing is disabled"
                CondenseResult.TargetOffline -> "that player is not online"
                CondenseResult.NothingToCondense -> "there was nothing to condense"
            }
        )

    // Workstations ---------------------------------------------------------------------------

    fun workstation(result: WorkstationResult): Component =
        prefixed(
            when (result) {
                WorkstationResult.Opened -> "opened"
                WorkstationResult.Disabled -> "portable workstations are disabled"
                WorkstationResult.NotAllowed -> "that workstation is not allowed"
                WorkstationResult.PlayerOffline -> "this command requires an online player"
                WorkstationResult.Failed -> "the workstation could not be opened"
            }
        )

    // Inspection -----------------------------------------------------------------------------

    fun inspection(result: InspectionOpenResult): Component =
        prefixed(
            when (result) {
                InspectionOpenResult.Opened -> "opened"
                InspectionOpenResult.PlayerOffline -> "that player is not online"
                InspectionOpenResult.Failed -> "that container could not be opened"
            }
        )

}
