package top.likoslupus.cellulosesz.utility.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.utility.item.*
import top.likoslupus.cellulosesz.utility.itemtools.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal fun UtilityMessages.repair(result: RepairResult): List<Notice> =
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

internal fun UtilityMessages.more(result: MoreResult): Notice =
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

internal fun UtilityMessages.condense(result: CondenseResult): Notice =
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

internal fun UtilityMessages.itemName(result: ItemNameResult): Notice =
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

internal fun UtilityMessages.itemLore(result: ItemLoreResult): Notice =
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

internal fun UtilityMessages.hat(result: HatResult): Notice =
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

internal fun UtilityMessages.clearRequest(result: ClearRequestResult): Notice =
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

internal fun UtilityMessages.clearInventory(result: ClearInventoryResult): Notice =
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

internal fun UtilityMessages.clearTargetNotice(scope: ClearScope): Notice =
    info(
        when (scope) {
            ClearScope.MAIN -> MessageKey("utility.item.clear.target_notice.main")
            ClearScope.ALL -> MessageKey("utility.item.clear.target_notice.all")
        }
    )

internal fun UtilityMessages.clearTargetDone(stacks: Int): Notice =
    info(
        MessageKey("utility.item.clear.target_done"),
        count(stacks)
    )

internal fun UtilityMessages.clearConfirmStatus(enabled: Boolean): Notice =
    info(
        when {
            enabled -> MessageKey("utility.item.clear.confirm_status.enabled")
            else -> MessageKey("utility.item.clear.confirm_status.disabled")
        }
    )

internal fun UtilityMessages.clearConfirmUpdated(enabled: Boolean): Notice =
    success(
        when {
            enabled -> MessageKey("utility.item.clear.confirm_enabled")
            else -> MessageKey("utility.item.clear.confirm_disabled")
        }
    )

internal fun UtilityMessages.preferenceStorageUnavailable(): Notice =
    notice(MessageKey("utility.item.clear.confirm_storage"))

internal fun UtilityMessages.give(result: GiveResult): List<Notice> =
    when (result) {
        is GiveResult.Given -> buildList {
            add(
                success(
                    MessageKey("utility.give.success"),
                    identifier(result.targetName),
                    literal(result.itemName),
                    count(result.amount),
                )
            )
            if (result.dropped) {
                add(warning(MessageKey("utility.give.dropped")))
            }
        }

        GiveResult.Blacklisted -> listOf(notice(MessageKey("utility.give.blacklisted")))
        GiveResult.InvalidAmount -> listOf(notice(MessageKey("utility.give.invalid_amount")))
        GiveResult.InvalidItem -> listOf(notice(MessageKey("utility.item.invalid")))
        GiveResult.TargetOffline -> listOf(notice(MessageKey("utility.item.target_offline")))
        GiveResult.InventoryFull -> listOf(notice(MessageKey("utility.give.inventory_full")))
    }

internal fun UtilityMessages.enchant(result: EnchantResult): Notice =
    notice(
        when (result) {
            is EnchantResult.Applied ->
                return success(
                    MessageKey("utility.enchant.applied"),
                    literal(result.enchantment),
                    count(result.level),
                    identifier(result.targetName),
                )

            is EnchantResult.Removed ->
                return success(
                    MessageKey("utility.enchant.removed"),
                    literal(result.enchantment),
                    identifier(result.targetName),
                )

            EnchantResult.UnknownEnchantment -> MessageKey("utility.enchant.unknown")
            EnchantResult.UnsafeDenied -> MessageKey("utility.enchant.unsafe")
            EnchantResult.InvalidLevel -> MessageKey("utility.enchant.invalid_level")
            EnchantResult.NoHeldItem -> MessageKey("utility.item.no_item")
            EnchantResult.NotSupported -> MessageKey("utility.enchant.not_supported")
            EnchantResult.TargetOffline -> MessageKey("utility.item.target_offline")
        }
    )

internal fun UtilityMessages.potion(result: PotionResult): Notice =
    notice(
        when (result) {
            is PotionResult.Cleared -> return success(MessageKey("utility.potion.cleared"))
            is PotionResult.Edited ->
                return success(
                    MessageKey("utility.potion.edited"),
                    count(result.effectCount),
                )

            is PotionResult.Applied ->
                return success(
                    MessageKey("utility.potion.applied"),
                    identifier(result.targetName),
                    count(result.effectCount),
                )

            PotionResult.UnknownEffect -> MessageKey("utility.potion.unknown_effect")
            PotionResult.NotPotion -> MessageKey("utility.potion.not_potion")
            PotionResult.InvalidSpec -> MessageKey("utility.potion.invalid")
            PotionResult.NoHeldItem -> MessageKey("utility.item.no_item")
            PotionResult.TargetOffline -> MessageKey("utility.item.target_offline")
        }
    )

internal fun UtilityMessages.skull(result: SkullResult): Notice =
    notice(
        when (result) {
            is SkullResult.Spawned ->
                return success(
                    MessageKey("utility.skull.spawned"),
                    literal(result.owner),
                    identifier(result.recipient),
                )

            is SkullResult.Edited ->
                return success(
                    MessageKey("utility.skull.edited"),
                    literal(result.owner),
                )

            SkullResult.NoHeldItem -> MessageKey("utility.skull.no_item")
            SkullResult.TargetOffline -> MessageKey("utility.item.target_offline")
            SkullResult.InventoryFull -> MessageKey("utility.skull.inventory_full")
        }
    )

internal fun UtilityMessages.skullDenied(): Notice =
    notice(MessageKey("utility.skull.denied"))

internal fun UtilityMessages.book(result: BookResult): Notice =
    when (result) {
        is BookResult.Converted -> success(
            when (result.nowWritten) {
                true -> MessageKey("utility.book.converted_written")
                false -> MessageKey("utility.book.converted_writable")
            }
        )

        is BookResult.AuthorSet ->
            success(
                MessageKey("utility.book.author"),
                literal(result.author)
            )

        is BookResult.TitleSet ->
            success(
                MessageKey("utility.book.title"),
                literal(result.title)
            )

        BookResult.NoHeldItem -> notice(MessageKey("utility.item.no_item"))
        BookResult.NotBook -> notice(MessageKey("utility.book.not_book"))
        BookResult.NotWrittenBook -> notice(MessageKey("utility.book.not_written"))
        BookResult.TargetOffline -> notice(MessageKey("utility.item.target_offline"))
    }

internal fun UtilityMessages.itemInfo(info: ItemInfo): DetailView =
    DetailView(
        title = LocalizedText(
            MessageKey("utility.itemdb.title"),
            listOf(literal(info.displayName)),
        ),
        fields = buildList {
            add(
                field(
                    MessageKey("utility.itemdb.field.id"),
                    LocalizedText(
                        MessageKey("core.value"),
                        listOf(identifier(info.registryId))
                    ),
                )
            )
            add(
                field(
                    MessageKey("utility.itemdb.field.stack"),
                    LocalizedText(
                        MessageKey("core.value"),
                        listOf(count(info.maxStackSize))
                    ),
                )
            )
            add(
                field(
                    MessageKey("utility.itemdb.field.aliases"),
                    when (info.aliases.isEmpty()) {
                        true -> LocalizedText(MessageKey("utility.itemdb.no_aliases"))
                        false -> LocalizedText(
                            MessageKey("core.value"),
                            listOf(literal(info.aliases.joinToString(", ")))
                        )
                    },
                )
            )
            add(
                field(
                    MessageKey("utility.itemdb.field.components"),
                    when (info.components.isEmpty()) {
                        true -> LocalizedText(MessageKey("utility.itemdb.no_components"))
                        false -> LocalizedText(
                            MessageKey("core.value"),
                            listOf(
                                literal(
                                    info.components.joinToString(", ") +
                                            " (+${info.componentCount - info.components.size})"
                                )
                            )
                        )
                    },
                )
            )
            info.durability?.let { durability ->
                add(
                    field(
                        MessageKey("utility.itemdb.field.durability"),
                        LocalizedText(
                            MessageKey("utility.itemdb.durability"),
                            listOf(
                                count(durability.maxDamage - durability.damage),
                                count(durability.maxDamage)
                            )
                        ),
                    )
                )
            }
        },
    )

internal fun UtilityMessages.itemInfoMissing(): Notice =
    notice(MessageKey("utility.itemdb.no_item"))

internal fun UtilityMessages.recipes(
    itemName: String,
    entries: List<RecipeView>,
    pageIndex: Int,
    pageCount: Int,
): ListPage =
    ListPage(
        title = LocalizedText(
            MessageKey("utility.recipe.title"),
            listOf(literal(itemName)),
        ),
        rows = entries.map { view ->
            ListRow(
                main = LocalizedText(
                    MessageKey("utility.recipe.row"),
                    listOf(count(view.resultCount), literal(view.result)),
                ),
                supporting = LocalizedText(
                    MessageKey("utility.recipe.ingredients"),
                    listOf(
                        literal(
                            view.ingredients
                                    .filter { it.isNotBlank() }
                                    .joinToString(", ")
                        )
                    ),
                ),
            )
        },
        emptyText = LocalizedText(MessageKey("utility.recipe.empty")),
        summary = LocalizedText(
            MessageKey("utility.recipe.page"),
            listOf(count(pageIndex), count(pageCount)),
        ),
    )

private fun UtilityMessages.seconds(value: Int): Duration =
    value.coerceAtLeast(0).seconds
