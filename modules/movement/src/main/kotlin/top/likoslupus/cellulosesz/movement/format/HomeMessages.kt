package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.ListRow
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.movement.home.*

internal fun MovementMessages.teleportToHome(home: Home): Notice =
    success(
        MessageKey("movement.home.teleported"),
        identifier(home.name.value)
    )

internal fun MovementMessages.homeSet(result: SetHomeResult): Notice =
    when (result) {
        SetHomeResult.Success ->
            success(MessageKey("movement.home.set"))

        SetHomeResult.InvalidName ->
            notice(MessageKey("movement.home.invalid_name"))

        SetHomeResult.LimitReached ->
            notice(MessageKey("movement.home.limit_reached"))
    }

internal fun MovementMessages.homeLookupError(result: HomeLookupResult): Notice? =
    when (result) {
        is HomeLookupResult.Found ->
            null

        HomeLookupResult.InvalidName ->
            notice(MessageKey("movement.home.invalid_name"))

        HomeLookupResult.NotFound ->
            notice(MessageKey("movement.home.not_found"))
    }

internal fun MovementMessages.homeDelete(name: String, result: DeleteHomeResult): Notice =
    when (result) {
        DeleteHomeResult.Deleted ->
            success(
                MessageKey("movement.home.deleted"),
                identifier(name)
            )

        DeleteHomeResult.InvalidName ->
            notice(MessageKey("movement.home.invalid_name"))

        DeleteHomeResult.NotFound ->
            notice(
                MessageKey("movement.home.delete_not_found"),
                identifier(name)
            )
    }

internal fun MovementMessages.homes(homes: List<Home>): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("movement.home.list_title")),
        rows = homes.map { home ->
            ListRow(
                main = LocalizedText(
                    MessageKey("core.value"),
                    listOf(identifier(home.name.value))
                )
            )
        },
        emptyText = LocalizedText(MessageKey("movement.home.list_empty")),
    )

internal fun MovementMessages.homeRename(
    from: String,
    to: String,
    result: RenameHomeResult
): Notice =
    when (result) {
        RenameHomeResult.Renamed ->
            success(
                MessageKey("movement.home.renamed"),
                identifier(from),
                identifier(to),
            )

        RenameHomeResult.InvalidName ->
            notice(MessageKey("movement.home.invalid_name"))

        RenameHomeResult.SourceNotFound ->
            notice(
                MessageKey("movement.home.rename_source_not_found"),
                identifier(from),
            )

        RenameHomeResult.DestinationExists ->
            notice(
                MessageKey("movement.home.rename_destination_exists"),
                identifier(to),
            )
    }

internal fun MovementMessages.homeWorldDenied(dimension: String): Notice =
    notice(
        MessageKey("movement.home.world_denied"),
        identifier(dimension),
    )

internal fun MovementMessages.homeOverwritePrompt(name: String): Notice =
    notice(
        MessageKey("movement.home.overwrite_prompt"),
        identifier(name),
    )
