package top.likoslupus.cellulosesz.utility.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.ListRow
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.utility.kit.*
import kotlin.time.toKotlinDuration

internal fun UtilityMessages.claim(requestedName: String, result: ClaimKitResult): List<Notice> =
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

internal fun UtilityMessages.create(result: CreateKitResult): Notice =
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

internal fun UtilityMessages.update(result: UpdateKitResult): Notice =
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

internal fun UtilityMessages.delete(result: DeleteKitResult): Notice =
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

internal fun UtilityMessages.list(result: ListKitsResult): ListPage =
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

internal fun UtilityMessages.show(result: ShowKitResult): ListPage =
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

internal fun UtilityMessages.reset(result: ResetKitResult): Notice =
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

private fun UtilityMessages.rows(entry: KitListEntry): List<ListRow> =
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

private fun UtilityMessages.reuse(reuse: KitReusePolicy): LocalizedText =
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

private fun UtilityMessages.availability(availability: KitAvailability): LocalizedText =
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

private fun UtilityMessages.showPage(definition: KitDefinition): ListPage =
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

private fun UtilityMessages.messagePage(key: MessageKey): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("utility.kit.list_title")),
        rows = emptyList(),
        emptyText = LocalizedText(key),
    )
