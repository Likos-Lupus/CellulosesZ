package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.player.presence.PlayerPresence
import java.time.Instant
import kotlin.time.Duration

internal fun PlayerMessages.seen(presence: PlayerPresence, now: Instant): Notice =
    when {
        presence.online -> info(
            MessageKey("player.seen.online"),
            playerName(presence.name)
        )

        presence.lastSeen != null -> info(
            MessageKey("player.seen.offline"),
            playerName(presence.name),
            elapsed(since(presence.lastSeen, now))
        )

        else -> info(
            MessageKey("player.seen.offline_unknown"),
            playerName(presence.name)
        )
    }

internal fun PlayerMessages.playtimeSelf(duration: Duration): Notice =
    info(
        MessageKey("player.playtime.self"),
        elapsed(duration)
    )

internal fun PlayerMessages.playtimeOther(name: String, duration: Duration): Notice =
    info(
        MessageKey("player.playtime.other"),
        playerName(name),
        elapsed(duration)
    )

internal fun PlayerMessages.realnameFound(nickname: String, actualName: String): Notice =
    info(
        MessageKey("player.realname.found"),
        literal(nickname),
        playerName(actualName)
    )

internal fun PlayerMessages.realnameNotFound(nickname: String): Notice =
    error(
        MessageKey("player.realname.not_found"),
        literal(nickname)
    )
