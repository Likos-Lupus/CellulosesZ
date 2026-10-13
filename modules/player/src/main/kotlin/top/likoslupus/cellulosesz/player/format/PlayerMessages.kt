package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.player.afk.AfkChange
import top.likoslupus.cellulosesz.player.nearby.NearbyResult
import top.likoslupus.cellulosesz.player.nickname.NicknameCurrent
import top.likoslupus.cellulosesz.player.nickname.NicknameForResult
import top.likoslupus.cellulosesz.player.nickname.NicknameSetResult
import top.likoslupus.cellulosesz.player.presence.PlayerPresence
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Maps player domain values to semantic output. It holds no language and no colour: the presentation
 * layer renders per recipient.
 */
internal object PlayerMessages {

    fun seen(presence: PlayerPresence, now: Instant): Notice =
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

    fun playtimeSelf(duration: Duration): Notice =
        info(
            MessageKey("player.playtime.self"),
            elapsed(duration)
        )

    fun playtimeOther(name: String, duration: Duration): Notice =
        info(
            MessageKey("player.playtime.other"),
            playerName(name),
            elapsed(duration)
        )

    fun whois(presence: PlayerPresence, now: Instant): DetailView =
        DetailView(
            title = LocalizedText(
                MessageKey("player.whois.title"),
                listOf(playerName(presence.name))
            ),
            fields = listOf(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.name")),
                    value = text(presence.name),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.uuid")),
                    value = text(presence.id.toString()),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.first_seen")),
                    value = agoOrUnknown(presence.firstSeen, now),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.last_seen")),
                    value = agoOrUnknown(presence.lastSeen, now),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.playtime")),
                    value = LocalizedText(
                        MessageKey("player.whois.value.duration"),
                        listOf(elapsed(presence.playtime))
                    ),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.online")),
                    value = LocalizedText(
                        when {
                            presence.online -> MessageKey("player.whois.value.online")
                            else -> MessageKey("player.whois.value.offline")
                        }
                    ),
                ),
            ),
        )

    fun notFound(name: String): Notice =
        error(
            MessageKey("player.player_not_found"),
            literal(name)
        )

    fun storageUnavailable(): Notice =
        error(MessageKey("player.storage_unavailable"))

    fun afkToggle(change: AfkChange): Notice =
        when {
            !change.afk -> info(MessageKey("player.afk.disabled"))
            change.reason == null -> success(MessageKey("player.afk.enabled"))
            else -> success(
                MessageKey("player.afk.enabled_reason"),
                literal(change.reason)
            )
        }

    fun afkDisabled(): Notice =
        info(MessageKey("player.afk.disabled_feature"))

    fun afkAnnouncement(change: AfkChange): Notice =
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

    fun ping(latencyMs: Int): Notice =
        info(
            MessageKey("player.ping.value"),
            count(latencyMs)
        )

    fun pingOther(name: String, latencyMs: Int): Notice =
        info(
            MessageKey("player.ping.other"),
            playerName(name),
            count(latencyMs)
        )

    fun near(result: NearbyResult): ListPage =
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

    fun nicknameFeedback(result: NicknameSetResult): Notice =
        when (result) {
            is NicknameSetResult.Set -> success(
                MessageKey("player.nickname.set"),
                literal(result.display)
            )

            NicknameSetResult.Cleared -> success(MessageKey("player.nickname.cleared"))
            NicknameSetResult.Disabled -> error(MessageKey("player.nickname.disabled"))
            NicknameSetResult.Empty -> error(MessageKey("player.nickname.empty"))
            is NicknameSetResult.TooShort -> error(
                MessageKey("player.nickname.too_short"),
                count(result.minimum)
            )

            is NicknameSetResult.TooLong -> error(
                MessageKey("player.nickname.too_long"),
                count(result.maximum)
            )

            NicknameSetResult.InvalidCharacters ->
                error(MessageKey("player.nickname.invalid_characters"))

            NicknameSetResult.Taken -> error(MessageKey("player.nickname.taken"))
            NicknameSetResult.Reserved -> error(MessageKey("player.nickname.reserved"))
            NicknameSetResult.StorageUnavailable -> storageUnavailable()
        }

    /** The bare `/nick` status line: the current nickname, or none. */
    fun nicknameSelfStatus(current: NicknameCurrent): Notice =
        when (current) {
            is NicknameCurrent.Set -> info(
                MessageKey("player.nickname.status.set"),
                literal(current.display)
            )

            NicknameCurrent.None -> info(MessageKey("player.nickname.status.none"))
            NicknameCurrent.StorageUnavailable -> storageUnavailable()
        }

    /** The subcommand list shown next to the bare `/nick` status line. */
    fun nicknameUsage(): Notice =
        info(MessageKey("player.nickname.usage"))

    /** Feedback for a staff `<setfor|clearfor>` change against a resolved target. */
    fun nicknameFor(targetName: String, result: NicknameForResult): Notice =
        when (result) {
            is NicknameForResult.Applied -> when (val outcome = result.result) {
                is NicknameSetResult.Set -> success(
                    MessageKey("player.nickname.admin.set"),
                    playerName(result.targetName),
                    literal(outcome.display)
                )

                NicknameSetResult.Cleared -> success(
                    MessageKey("player.nickname.admin.cleared"),
                    playerName(result.targetName)
                )

                else -> nicknameFeedback(outcome)
            }

            NicknameForResult.TargetNotFound -> notFound(targetName)
        }

    fun realnameFound(nickname: String, actualName: String): Notice =
        info(
            MessageKey("player.realname.found"),
            literal(nickname),
            playerName(actualName)
        )

    fun realnameNotFound(nickname: String): Notice =
        error(
            MessageKey("player.realname.not_found"),
            literal(nickname)
        )

    private fun agoOrUnknown(at: Instant?, now: Instant): LocalizedText =
        if (at == null) {
            LocalizedText(MessageKey("player.whois.value.unknown"))
        } else {
            LocalizedText(
                MessageKey("player.whois.value.ago"),
                listOf(elapsed(since(at, now)))
            )
        }

    private fun since(at: Instant, now: Instant): Duration =
        (now.toEpochMilli() - at.toEpochMilli()).coerceAtLeast(0L).milliseconds

    private fun text(value: String): LocalizedText =
        LocalizedText(MessageKey("core.value"), listOf(playerName(value)))

    private fun notice(
        key: MessageKey,
        tone: NoticeTone,
        vararg arguments: TextArgument,
    ): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            tone
        )

    private fun info(key: MessageKey, vararg arguments: TextArgument): Notice =
        notice(key, NoticeTone.INFO, *arguments)

    private fun success(key: MessageKey, vararg arguments: TextArgument): Notice =
        notice(key, NoticeTone.SUCCESS, *arguments)

    private fun error(key: MessageKey, vararg arguments: TextArgument): Notice =
        notice(key, NoticeTone.ERROR, *arguments)

    private fun playerName(value: String): TextArgument =
        TextArgument.PlayerName(value)

    private fun literal(value: String): TextArgument =
        TextArgument.Literal(value)

    private fun count(value: Int): TextArgument =
        TextArgument.Count(value.toLong())

    private fun count(value: Double): TextArgument =
        TextArgument.Count(value.toLong())

    private fun elapsed(value: Duration): TextArgument =
        TextArgument.Elapsed(value)

}
