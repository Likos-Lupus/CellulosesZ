package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.player.nearby.NearbyResult

internal fun PlayerMessages.ping(latencyMs: Int): Notice =
    info(
        MessageKey("player.ping.value"),
        count(latencyMs)
    )

internal fun PlayerMessages.pingOther(name: String, latencyMs: Int): Notice =
    info(
        MessageKey("player.ping.other"),
        playerName(name),
        count(latencyMs)
    )

internal fun PlayerMessages.near(result: NearbyResult): ListPage =
    when (result) {
        is NearbyResult.Found -> ListPage(
            title = LocalizedText(MessageKey("player.near.title")),
            rows = result.players.map { player ->
                ListRow(
                    main = text(player.name),
                    supporting = LocalizedText(
                        MessageKey("player.near.distance"),
                        listOf(count(player.distance))
                    ),
                )
            },
            emptyText = LocalizedText(
                MessageKey("player.near.empty"),
                listOf(count(result.radius))
            ),
            summary = LocalizedText(
                MessageKey("player.near.summary"),
                listOf(
                    count(result.players.size),
                    count(result.radius)
                )
            ),
        )

        NearbyResult.PlayerOffline -> ListPage(
            title = LocalizedText(MessageKey("player.near.title")),
            rows = emptyList(),
            emptyText = LocalizedText(MessageKey("player.near.unavailable")),
        )

        is NearbyResult.RadiusTooLarge -> ListPage(
            title = LocalizedText(MessageKey("player.near.title")),
            rows = emptyList(),
            emptyText = LocalizedText(
                MessageKey("player.near.radius_too_large"),
                listOf(count(result.maximum))
            ),
        )
    }
