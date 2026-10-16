package top.likoslupus.cellulosesz.utility.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.utility.inspection.InspectionOpenResult
import top.likoslupus.cellulosesz.utility.item.*
import top.likoslupus.cellulosesz.utility.kit.*
import top.likoslupus.cellulosesz.utility.position.DepthReading
import top.likoslupus.cellulosesz.utility.workstation.WorkstationResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/**
 * Maps utility domain results to semantic values. It holds no language and no colour: the
 * presentation layer renders per recipient.
 */
internal object UtilityMessages {

    fun claim(requestedName: String, result: ClaimKitResult): List<Notice> =
        listOf(
            when (result) {
                is ClaimKitResult.Delivered ->
                    return buildList {
                        add(
                            success(
                                MessageKey("utility.kit.claim.success"),
                                identifier(result.kit.value)
                            )
                        )
                        if (result.droppedStacks > 0) {
                            add(
                                warning(
                                    MessageKey("utility.kit.claim.dropped"),
                                    count(result.droppedStacks)
                                )
                            )
                        }
                        if (!result.claimRecordedFully) {
                            add(warning(MessageKey("utility.kit.claim.metadata_warning")))
                        }
                    }

                ClaimKitResult.Disabled ->
                    notice(MessageKey("utility.kit.disabled"))

                ClaimKitResult.CatalogLoading ->
                    notice(MessageKey("utility.kit.catalog_loading"))

                ClaimKitResult.StorageUnavailable ->
                    notice(MessageKey("utility.kit.storage_unavailable"))

                ClaimKitResult.InvalidName ->
                    notice(MessageKey("utility.kit.invalid_name"))

                ClaimKitResult.NotFound ->
                    notice(
                        MessageKey("utility.kit.not_found"),
                        identifier(requestedName)
                    )

                ClaimKitResult.PlayerOffline ->
                    notice(MessageKey("utility.kit.player_offline"))

                ClaimKitResult.InventoryFull ->
                    notice(MessageKey("utility.kit.inventory_full"))

                is ClaimKitResult.Cooldown ->
                    notice(
                        MessageKey("utility.kit.cooldown"),
                        identifier(requestedName),
                        elapsed(result.remaining.toKotlinDuration()),
                    )

                ClaimKitResult.AlreadyUsed ->
                    notice(
                        MessageKey("utility.kit.already_used"),
                        identifier(requestedName)
                    )

                ClaimKitResult.DeliveryFailedAfterReservation ->
                    notice(MessageKey("utility.kit.delivery_failed"))

                is ClaimKitResult.Denied ->
                    notice(
                        MessageKey("utility.kit.denied"),
                        identifier(requestedName)
                    )
            }
        )

    fun create(result: CreateKitResult): Notice =
        notice(
            when (result) {
                is CreateKitResult.Created ->
                    return success(
                        MessageKey("utility.kit.created"),
                        identifier(result.definition.name.value)
                    )

                CreateKitResult.Disabled -> MessageKey("utility.kit.disabled")
                CreateKitResult.InvalidName -> MessageKey("utility.kit.invalid_name")
                CreateKitResult.AlreadyExists -> MessageKey("utility.kit.exists")
                CreateKitResult.InvalidCooldown -> MessageKey("utility.kit.invalid_cooldown")
                CreateKitResult.CooldownTooLong -> MessageKey("utility.kit.cooldown_too_long")
                CreateKitResult.EmptyInventory -> MessageKey("utility.kit.empty_inventory")
                CreateKitResult.TooManyItems -> MessageKey("utility.kit.too_many_items")
                CreateKitResult.KitLimitReached -> MessageKey("utility.kit.limit_reached")
                CreateKitResult.PlayerOffline -> MessageKey("utility.kit.player_offline")
                CreateKitResult.StorageUnavailable -> MessageKey("utility.kit.storage_unavailable")
            }
        )

    fun update(result: UpdateKitResult): Notice =
        notice(
            when (result) {
                is UpdateKitResult.Updated ->
                    return success(
                        MessageKey("utility.kit.updated"),
                        identifier(result.definition.name.value)
                    )

                UpdateKitResult.Disabled -> MessageKey("utility.kit.disabled")
                UpdateKitResult.InvalidName -> MessageKey("utility.kit.invalid_name")
                UpdateKitResult.NotFound -> MessageKey("utility.kit.does_not_exist")
                UpdateKitResult.InvalidCooldown -> MessageKey("utility.kit.invalid_cooldown")
                UpdateKitResult.CooldownTooLong -> MessageKey("utility.kit.cooldown_too_long")
                UpdateKitResult.EmptyInventory -> MessageKey("utility.kit.empty_inventory")
                UpdateKitResult.TooManyItems -> MessageKey("utility.kit.too_many_items")
                UpdateKitResult.PlayerOffline -> MessageKey("utility.kit.player_offline")
                UpdateKitResult.StorageUnavailable -> MessageKey("utility.kit.storage_unavailable")
            }
        )

    fun delete(result: DeleteKitResult): Notice =
        notice(
            when (result) {
                is DeleteKitResult.Deleted ->
                    return success(
                        MessageKey("utility.kit.deleted"),
                        identifier(result.name.value)
                    )

                DeleteKitResult.Disabled -> MessageKey("utility.kit.disabled")
                DeleteKitResult.InvalidName -> MessageKey("utility.kit.invalid_name")
                DeleteKitResult.NotFound -> MessageKey("utility.kit.does_not_exist")
                DeleteKitResult.StorageUnavailable -> MessageKey("utility.kit.storage_unavailable")
            }
        )

    fun list(result: ListKitsResult): ListPage =
        when (result) {
            is ListKitsResult.Listed -> ListPage(
                title = LocalizedText(MessageKey("utility.kit.list_title")),
                rows = result.entries.flatMap { entry -> rows(entry) },
                emptyText = LocalizedText(MessageKey("utility.kit.list_empty")),
            )

            ListKitsResult.Loading -> ListPage(
                title = LocalizedText(MessageKey("utility.kit.list_title")),
                rows = emptyList(),
                emptyText = LocalizedText(MessageKey("utility.kit.catalog_loading")),
            )

            ListKitsResult.StorageUnavailable -> ListPage(
                title = LocalizedText(MessageKey("utility.kit.list_title")),
                rows = emptyList(),
                emptyText = LocalizedText(MessageKey("utility.kit.storage_unavailable")),
            )
        }

    fun show(result: ShowKitResult): ListPage =
        messagePage(
            when (result) {
                is ShowKitResult.Shown -> return showPage(result.definition)
                ShowKitResult.Disabled -> MessageKey("utility.kit.disabled")
                ShowKitResult.Loading -> MessageKey("utility.kit.catalog_loading")
                ShowKitResult.StorageUnavailable -> MessageKey("utility.kit.storage_unavailable")
                ShowKitResult.InvalidName -> MessageKey("utility.kit.invalid_name")
                ShowKitResult.NotFound -> MessageKey("utility.kit.does_not_exist")
            }
        )

    fun reset(result: ResetKitResult): Notice =
        notice(
            when (result) {
                is ResetKitResult.Reset ->
                    return success(
                        MessageKey("utility.kit.reset"),
                        identifier(result.name.value),
                        identifier(result.target.name)
                    )

                ResetKitResult.NothingToReset -> MessageKey("utility.kit.nothing_to_reset")
                ResetKitResult.Disabled -> MessageKey("utility.kit.disabled")
                ResetKitResult.InvalidName -> MessageKey("utility.kit.invalid_name")
                ResetKitResult.NotFound -> MessageKey("utility.kit.does_not_exist")
                ResetKitResult.StorageUnavailable -> MessageKey("utility.kit.storage_unavailable")
            }
        )

    fun repair(result: RepairResult): List<Notice> =
        listOf(
            notice(
                when (result) {
                    is RepairResult.Repaired ->
                        return buildList {
                            add(
                                success(
                                    MessageKey("utility.item.repair"),
                                    count(result.count),
                                    identifier(result.playerName)
                                )
                            )
                            if (result.skippedEnchanted > 0) {
                                add(
                                    warning(
                                        MessageKey("utility.item.repair_skipped"),
                                        count(result.skippedEnchanted)
                                    )
                                )
                            }
                        }

                    RepairResult.Disabled -> MessageKey("utility.item.repair_disabled")
                    RepairResult.TargetOffline -> MessageKey("utility.item.target_offline")
                    RepairResult.NothingToRepair -> MessageKey("utility.item.nothing_to_repair")
                }
            )
        )

    fun more(result: MoreResult): Notice =
        notice(
            when (result) {
                is MoreResult.Filled ->
                    return success(
                        MessageKey("utility.item.more"),
                        identifier(result.itemName),
                        identifier(result.playerName)
                    )

                MoreResult.Disabled -> MessageKey("utility.item.more_disabled")
                MoreResult.TargetOffline -> MessageKey("utility.item.target_offline")
                MoreResult.NothingToFill -> MessageKey("utility.item.nothing_to_fill")
            }
        )

    fun condense(result: CondenseResult): Notice =
        notice(
            when (result) {
                is CondenseResult.Condensed ->
                    return success(
                        MessageKey("utility.item.condense"),
                        count(result.conversions)
                    )

                CondenseResult.Disabled -> MessageKey("utility.item.condense_disabled")
                CondenseResult.TargetOffline -> MessageKey("utility.item.target_offline")
                CondenseResult.NothingToCondense -> MessageKey("utility.item.nothing_to_condense")
            }
        )

    fun itemName(result: ItemNameResult): Notice =
        notice(
            when (result) {
                is ItemNameResult.Set ->
                    return success(
                        MessageKey("utility.item.name.set"),
                        literal(result.displayName)
                    )

                is ItemNameResult.Cleared ->
                    return success(MessageKey("utility.item.name.cleared"))

                is ItemNameResult.TooLong ->
                    return notice(
                        MessageKey("utility.item.name.too_long"),
                        count(result.limit)
                    )

                ItemNameResult.TargetOffline -> MessageKey("utility.item.target_offline")
                ItemNameResult.NoHeldItem -> MessageKey("utility.item.no_item")
            }
        )

    fun itemLore(result: ItemLoreResult): Notice =
        notice(
            when (result) {
                is ItemLoreResult.Set ->
                    return success(
                        MessageKey("utility.item.lore.set"),
                        count(result.lines)
                    )

                is ItemLoreResult.Cleared ->
                    return success(MessageKey("utility.item.lore.cleared"))

                is ItemLoreResult.TooManyLines ->
                    return notice(
                        MessageKey("utility.item.lore.too_many"),
                        count(result.limit)
                    )

                is ItemLoreResult.LineTooLong ->
                    return notice(
                        MessageKey("utility.item.lore.line_too_long"),
                        count(result.limit)
                    )

                ItemLoreResult.TargetOffline -> MessageKey("utility.item.target_offline")
                ItemLoreResult.NoHeldItem -> MessageKey("utility.item.no_item")
            }
        )

    fun hat(result: HatResult): Notice =
        notice(
            when (result) {
                is HatResult.Worn ->
                    return success(
                        MessageKey("utility.item.hat.worn"),
                        literal(result.itemName)
                    )

                HatResult.NotAllowed -> MessageKey("utility.item.hat.not_allowed")
                HatResult.TargetOffline -> MessageKey("utility.item.target_offline")
                HatResult.NoHeldItem -> MessageKey("utility.item.no_item")
            }
        )

    fun clearRequest(result: ClearRequestResult): Notice =
        notice(
            when (result) {
                is ClearRequestResult.Issued -> when (val target = result.targetName) {
                    null -> return info(
                        MessageKey("utility.item.clear.request"),
                        literal(result.code),
                        count(result.stacks),
                        elapsed(seconds(result.seconds)),
                    )

                    else -> return info(
                        MessageKey("utility.item.clear.request_other"),
                        identifier(target),
                        count(result.stacks),
                        literal(result.code),
                        elapsed(seconds(result.seconds)),
                    )
                }

                ClearRequestResult.TargetOffline -> MessageKey("utility.item.target_offline")
            }
        )

    fun clearInventory(result: ClearInventoryResult): Notice =
        notice(
            when (result) {
                is ClearInventoryResult.Cleared -> when (val target = result.targetName) {
                    null -> return success(
                        MessageKey("utility.item.clear.done"),
                        count(result.stacks),
                    )

                    else -> return success(
                        MessageKey("utility.item.clear.done_other"),
                        count(result.stacks),
                        identifier(target),
                    )
                }

                ClearInventoryResult.InvalidCode -> MessageKey("utility.item.clear.invalid")
                ClearInventoryResult.TargetOffline -> MessageKey("utility.item.target_offline")
                ClearInventoryResult.InventoryChanged -> MessageKey("utility.item.clear.changed")
            }
        )

    fun clearTargetNotice(scope: ClearScope): Notice =
        info(
            when (scope) {
                ClearScope.MAIN -> MessageKey("utility.item.clear.target_notice.main")
                ClearScope.ALL -> MessageKey("utility.item.clear.target_notice.all")
            }
        )

    fun clearTargetDone(stacks: Int): Notice =
        info(
            MessageKey("utility.item.clear.target_done"),
            count(stacks)
        )

    fun position(
        dimension: String,
        x: Int,
        y: Int,
        z: Int,
    ): DetailView =
        DetailView(
            title = LocalizedText(MessageKey("utility.getpos.title")),
            fields = listOf(
                field(
                    MessageKey("utility.getpos.field.position"),
                    LocalizedText(
                        MessageKey("core.value"),
                        listOf(
                            position(
                                x,
                                y,
                                z,
                                dimension
                            )
                        ),
                    ),
                )
            ),
        )

    fun depth(reading: DepthReading): DetailView {
        val depthValue = when {
            reading.belowAnchor > 0 ->
                LocalizedText(
                    MessageKey("utility.depth.below"),
                    listOf(
                        count(reading.belowAnchor),
                        count(reading.anchor),
                    ),
                )

            reading.aboveAnchor > 0 ->
                LocalizedText(
                    MessageKey("utility.depth.above"),
                    listOf(
                        count(reading.aboveAnchor),
                        count(reading.anchor),
                    ),
                )

            else -> LocalizedText(
                MessageKey("utility.depth.at"),
                listOf(count(reading.anchor))
            )
        }
        return DetailView(
            title = LocalizedText(MessageKey("utility.depth.title")),
            fields = listOf(
                field(MessageKey("utility.depth.field.depth"), depthValue),
                field(
                    MessageKey("utility.depth.field.range"),
                    LocalizedText(
                        MessageKey("utility.depth.range"),
                        listOf(
                            count(reading.minY),
                            count(reading.maxY),
                        ),
                    ),
                ),
            ),
        )
    }

    fun workstation(result: WorkstationResult): Notice =
        notice(
            when (result) {
                WorkstationResult.Opened -> return info(MessageKey("utility.workstation.opened"))
                WorkstationResult.Disabled -> MessageKey("utility.workstation.disabled")
                WorkstationResult.NotAllowed -> MessageKey("utility.workstation.not_allowed")
                WorkstationResult.PlayerOffline -> MessageKey("utility.kit.player_offline")
                WorkstationResult.Failed -> MessageKey("utility.workstation.failed")
            }
        )

    fun inspection(result: InspectionOpenResult): Notice =
        notice(
            when (result) {
                InspectionOpenResult.Opened -> return info(MessageKey("utility.inspection.opened"))
                InspectionOpenResult.PlayerOffline -> MessageKey("utility.item.target_offline")
                InspectionOpenResult.Failed -> MessageKey("utility.inspection.failed")
            }
        )

    private fun rows(entry: KitListEntry): List<ListRow> =
        listOf(
            ListRow(
                main = LocalizedText(
                    MessageKey("core.value"),
                    listOf(identifier(entry.name.value))
                )
            ),
            ListRow(
                main = reuse(entry.reuse),
                supporting = availability(entry.availability),
            ),
        )

    private fun reuse(reuse: KitReusePolicy): LocalizedText =
        when (reuse) {
            KitReusePolicy.Always ->
                LocalizedText(MessageKey("utility.kit.reuse.always"))

            KitReusePolicy.Once ->
                LocalizedText(MessageKey("utility.kit.reuse.once"))

            is KitReusePolicy.Cooldown ->
                LocalizedText(
                    MessageKey("utility.kit.reuse.cooldown"),
                    listOf(elapsed(reuse.duration.toKotlinDuration()))
                )
        }

    private fun availability(availability: KitAvailability): LocalizedText =
        when (availability) {
            KitAvailability.Available ->
                LocalizedText(MessageKey("utility.kit.availability.available"))

            KitAvailability.Used ->
                LocalizedText(MessageKey("utility.kit.availability.used"))

            is KitAvailability.Cooldown ->
                LocalizedText(
                    MessageKey("utility.kit.availability.cooldown"),
                    listOf(elapsed(availability.remaining.toKotlinDuration())),
                )
        }

    private fun showPage(definition: KitDefinition): ListPage =
        ListPage(
            title = LocalizedText(
                MessageKey("core.value"),
                listOf(identifier(definition.name.value))
            ),
            rows = definition.items.map { stack ->
                ListRow(
                    main = LocalizedText(
                        MessageKey("utility.kit.show.item"),
                        listOf(
                            count(stack.count),
                            literal(stack.hoverName.string)
                        ),
                    )
                )
            },
            emptyText = LocalizedText(MessageKey("utility.kit.list_empty")),
            summary = reuse(definition.reuse),
        )

    private fun messagePage(key: MessageKey): ListPage =
        ListPage(
            title = LocalizedText(MessageKey("utility.kit.list_title")),
            rows = emptyList(),
            emptyText = LocalizedText(key),
        )

    private fun notice(
        key: MessageKey,
        vararg arguments: TextArgument
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.ERROR
        )

    private fun success(
        key: MessageKey,
        vararg arguments: TextArgument
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.SUCCESS
        )

    private fun warning(
        key: MessageKey,
        vararg arguments: TextArgument
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.WARNING
        )

    private fun info(
        key: MessageKey,
        vararg arguments: TextArgument
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.INFO
        )

    private fun literal(value: String): TextArgument =
        TextArgument.Literal(value)

    private fun identifier(value: String): TextArgument =
        TextArgument.Identifier(value)

    private fun count(value: Int): TextArgument =
        TextArgument.Count(value.toLong())

    private fun count(value: Long): TextArgument =
        TextArgument.Count(value)

    private fun elapsed(value: Duration): TextArgument =
        TextArgument.Elapsed(value)

    private fun seconds(value: Int): Duration =
        value.coerceAtLeast(0).seconds

    private fun position(
        x: Int,
        y: Int,
        z: Int,
        dimension: String,
    ): TextArgument =
        TextArgument.Position(
            x = x,
            y = y,
            z = z,
            dimension = dimension,
        )

    private fun field(label: MessageKey, value: LocalizedText): DetailField =
        DetailField(LocalizedText(label), value)

}
