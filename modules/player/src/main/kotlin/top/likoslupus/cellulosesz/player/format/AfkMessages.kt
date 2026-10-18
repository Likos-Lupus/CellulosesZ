package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.player.afk.AfkChange

internal fun PlayerMessages.afkToggle(change: AfkChange): Notice =
    when {
        !change.afk -> info(MessageKey("player.afk.disabled"))
        change.reason == null -> success(MessageKey("player.afk.enabled"))
        else -> success(
            MessageKey("player.afk.enabled_reason"),
            literal(change.reason)
        )
    }

internal fun PlayerMessages.afkDisabled(): Notice =
    info(MessageKey("player.afk.disabled_feature"))

internal fun PlayerMessages.afkAnnouncement(change: AfkChange): Notice =
    when {
        !change.afk -> info(
            MessageKey("player.afk.announce.off"),
            playerName(change.playerName)
        )

        change.reason == null -> info(
            MessageKey("player.afk.announce.on"),
            playerName(change.playerName)
        )

        else -> info(
            MessageKey("player.afk.announce.on_reason"),
            playerName(change.playerName),
            literal(change.reason)
        )
    }
