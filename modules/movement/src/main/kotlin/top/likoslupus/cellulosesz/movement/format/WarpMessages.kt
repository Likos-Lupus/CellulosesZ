package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.movement.warp.*
import java.time.Instant

internal fun MovementMessages.teleportToWarp(warp: Warp): Notice =
    success(
        MessageKey("movement.warp.teleported"),
        identifier(warp.name.value)
    )

internal fun MovementMessages.warpSet(name: String, result: SetWarpResult): Notice =
    when (result) {
        SetWarpResult.Success ->
            success(
                MessageKey("movement.warp.created"),
                identifier(name)
            )

        SetWarpResult.InvalidName ->
            notice(MessageKey("movement.warp.invalid_name"))
    }

internal fun MovementMessages.warpLookupError(result: WarpLookupResult): Notice? =
    when (result) {
        is WarpLookupResult.Found ->
            null

        WarpLookupResult.InvalidName ->
            notice(MessageKey("movement.warp.invalid_name"))

        WarpLookupResult.NotFound ->
            notice(MessageKey("movement.warp.not_found"))
    }

internal fun MovementMessages.warpDelete(name: String, result: DeleteWarpResult): Notice =
    when (result) {
        DeleteWarpResult.Deleted ->
            success(
                MessageKey("movement.warp.deleted"),
                identifier(name)
            )

        DeleteWarpResult.InvalidName ->
            notice(MessageKey("movement.warp.invalid_name"))

        DeleteWarpResult.NotFound ->
            notice(
                MessageKey("movement.warp.delete_not_found"),
                identifier(name)
            )
    }

internal fun MovementMessages.warps(warps: List<Warp>): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("movement.warp.list_title")),
        rows = warps.map { warp ->
            ListRow(
                main = LocalizedText(
                    MessageKey("core.value"),
                    listOf(identifier(warp.name.value))
                )
            )
        },
        emptyText = LocalizedText(MessageKey("movement.warp.list_empty")),
    )

internal fun MovementMessages.warpInfo(
    found: WarpInfoResult.Found,
    now: Instant,
    showCoordinates: Boolean,
): DetailView {
    val warp = found.warp
    val metadata = found.metadata
    val fields = buildList {
        add(
            DetailField(
                label = LocalizedText(MessageKey("movement.warp.info.field.dimension")),
                value = LocalizedText(
                    MessageKey("core.value"),
                    listOf(literal(warp.position.dimension)),
                ),
            )
        )
        if (!showCoordinates) {
            return@buildList
        }
        add(
            DetailField(
                label = LocalizedText(MessageKey("movement.warp.info.field.position")),
                value = LocalizedText(
                    MessageKey("core.position.dimension"),
                    listOf(blockPosition(warp.position)),
                ),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("movement.warp.info.field.created_by")),
                value = metadata
                        ?.let {
                            LocalizedText(
                                MessageKey("core.value"),
                                listOf(playerName(it.createdBy))
                            )
                        }
                    ?: LocalizedText(MessageKey("movement.warp.info.unknown")),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("movement.warp.info.field.created_at")),
                value = metadata
                        ?.let {
                            LocalizedText(
                                MessageKey("core.value"),
                                listOf(elapsed(ago(it.createdAt, now))),
                            )
                        }
                    ?: LocalizedText(MessageKey("movement.warp.info.unknown")),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("movement.warp.info.field.updated_at")),
                value = metadata
                        ?.updatedAt
                        ?.let {
                            LocalizedText(
                                MessageKey("core.value"),
                                listOf(elapsed(ago(it, now))),
                            )
                        }
                    ?: LocalizedText(MessageKey("movement.warp.info.unknown")),
            )
        )
    }
    return DetailView(
        title = LocalizedText(
            MessageKey("movement.warp.info.title"),
            listOf(identifier(warp.name.value)),
        ),
        fields = fields,
    )
}

internal fun MovementMessages.warpNotPermitted(name: String): Notice =
    notice(
        MessageKey("movement.warp.no_permission"),
        identifier(name),
    )
