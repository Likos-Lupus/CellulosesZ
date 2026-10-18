package top.likoslupus.cellulosesz.utility.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.ListRow
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.utility.unlimited.UnlimitedClearResult
import top.likoslupus.cellulosesz.utility.unlimited.UnlimitedListResult
import top.likoslupus.cellulosesz.utility.unlimited.UnlimitedToggleResult

internal fun UtilityMessages.unlimitedList(result: UnlimitedListResult): ListPage =
    when (result) {
        is UnlimitedListResult.Listed -> ListPage(
            title = LocalizedText(
                MessageKey("utility.unlimited.list_title"),
                listOf(identifier(result.playerName)),
            ),
            rows = result.items.map { itemId ->
                ListRow(
                    main = LocalizedText(
                        MessageKey("core.value"),
                        listOf(identifier(itemId))
                    )
                )
            },
            emptyText = LocalizedText(MessageKey("utility.unlimited.list_empty")),
        )

        UnlimitedListResult.TargetOffline -> ListPage(
            title = LocalizedText(MessageKey("utility.unlimited.list_title")),
            rows = emptyList(),
            emptyText = LocalizedText(MessageKey("utility.item.target_offline")),
        )

        UnlimitedListResult.StorageUnavailable -> ListPage(
            title = LocalizedText(MessageKey("utility.unlimited.list_title")),
            rows = emptyList(),
            emptyText = LocalizedText(MessageKey("utility.unlimited.storage")),
        )
    }

internal fun UtilityMessages.unlimitedToggle(result: UnlimitedToggleResult): Notice =
    when (result) {
        is UnlimitedToggleResult.Added ->
            success(
                MessageKey("utility.unlimited.added"),
                identifier(result.itemId),
                identifier(result.playerName),
            )

        is UnlimitedToggleResult.Removed ->
            success(
                MessageKey("utility.unlimited.removed"),
                identifier(result.itemId),
                identifier(result.playerName),
            )

        UnlimitedToggleResult.NoHeldItem -> notice(MessageKey("utility.unlimited.no_item"))
        UnlimitedToggleResult.TargetOffline -> notice(MessageKey("utility.item.target_offline"))
        UnlimitedToggleResult.StorageUnavailable -> notice(MessageKey("utility.unlimited.storage"))
    }

internal fun UtilityMessages.unlimitedClear(result: UnlimitedClearResult): Notice =
    when (result) {
        is UnlimitedClearResult.Cleared ->
            success(
                MessageKey("utility.unlimited.cleared"),
                count(result.removed),
                identifier(result.playerName),
            )

        UnlimitedClearResult.TargetOffline -> notice(MessageKey("utility.item.target_offline"))
        UnlimitedClearResult.StorageUnavailable -> notice(MessageKey("utility.unlimited.storage"))
    }
