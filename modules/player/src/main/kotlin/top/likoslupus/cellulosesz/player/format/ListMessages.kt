package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.player.list.PlayerListResult

internal fun PlayerMessages.listPage(result: PlayerListResult.Rows, afkMarker: String): ListPage {
    val rows = result.entries.map { entry ->
        val main = when {
            entry.afk -> LocalizedText(
                MessageKey("player.list.entry_afk"),
                listOf(
                    playerName(entry.name),
                    literal(afkMarker)
                ),
            )

            else -> LocalizedText(
                MessageKey("player.list.entry"),
                listOf(playerName(entry.name)),
            )
        }
        ListRow(
            main = main,
            supporting = LocalizedText(
                MessageKey("core.value"),
                listOf(literal(entry.groupLabel)),
            ),
        )
    }
    val summary = when (val label = result.filterLabel) {
        null -> LocalizedText(
            MessageKey("player.list.summary"),
            listOf(
                count(result.online),
                count(result.maximum)
            ),
        )

        else -> LocalizedText(
            MessageKey("player.list.summary_group"),
            listOf(
                literal(label),
                count(result.online),
                count(result.maximum)
            ),
        )
    }
    return ListPage(
        title = LocalizedText(MessageKey("player.list.title")),
        rows = rows,
        emptyText = LocalizedText(MessageKey("player.list.empty")),
        summary = summary,
    )
}

internal fun PlayerMessages.listUnknownGroup(raw: String): Notice =
    error(MessageKey("player.list.unknown_group"), literal(raw))
