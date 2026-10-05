package top.likoslupus.cellulosesz.utility.item

import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.utility.config.ItemUtilitySettings
import java.util.*

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

/**
 * Item utility orchestration. These are synchronous because command handlers already run on the
 * server thread and the work is immediate (no filesystem IO).
 */
internal class ItemUtilityService(
    private val backend: ItemUtilityBackend,
    private val known: KnownPlayerResolver,
    private val settings: () -> ItemUtilitySettings,
) {

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

}
