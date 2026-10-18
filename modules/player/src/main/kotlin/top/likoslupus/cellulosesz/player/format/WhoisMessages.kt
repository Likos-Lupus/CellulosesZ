package top.likoslupus.cellulosesz.player.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.DetailField
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.player.presence.PlayerPresence
import top.likoslupus.cellulosesz.player.whois.WhoisEnrichment
import top.likoslupus.cellulosesz.player.whois.WhoisLive
import java.time.Instant

internal fun PlayerMessages.whois(
    presence: PlayerPresence,
    now: Instant,
    enrichment: WhoisEnrichment? = null,
): DetailView =
    DetailView(
        title = LocalizedText(
            MessageKey("player.whois.title"),
            listOf(playerName(presence.name))
        ),
        fields = buildList {
            add(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.name")),
                    value = text(presence.name),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.uuid")),
                    value = text(presence.id.toString()),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.first_seen")),
                    value = agoOrUnknown(presence.firstSeen, now),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.last_seen")),
                    value = agoOrUnknown(presence.lastSeen, now),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.playtime")),
                    value = LocalizedText(
                        MessageKey("player.whois.value.duration"),
                        listOf(elapsed(presence.playtime))
                    ),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.online")),
                    value = LocalizedText(
                        when {
                            presence.online -> MessageKey("player.whois.value.online")
                            else -> MessageKey("player.whois.value.offline")
                        }
                    ),
                )
            )
            enrichment?.live?.let { live ->
                addAll(liveFields(live))
            }
            enrichment?.extra?.let(::addAll)
        },
    )

private fun PlayerMessages.liveFields(live: WhoisLive): List<DetailField> =
    buildList {
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.health")),
                value = LocalizedText(
                    MessageKey("player.whois.value.fraction"),
                    listOf(
                        literal(decimal(live.health)),
                        literal(decimal(live.maxHealth))
                    ),
                ),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.hunger")),
                value = LocalizedText(
                    MessageKey("player.whois.value.fraction"),
                    listOf(count(live.food), count(MAX_FOOD)),
                ),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.experience")),
                value = LocalizedText(
                    MessageKey("player.whois.value.experience"),
                    listOf(count(live.experienceLevel)),
                ),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.location")),
                value = LocalizedText(
                    MessageKey("core.value"),
                    listOf(
                        TextArgument.Position(
                            x = live.x,
                            y = live.y,
                            z = live.z,
                            dimension = live.dimension,
                        )
                    ),
                ),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.game_mode")),
                value = text(live.gameMode),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.operator")),
                value = yesNo(live.operator),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.whitelisted")),
                value = yesNo(live.whitelisted),
            )
        )
        add(
            DetailField(
                label = LocalizedText(MessageKey("player.whois.field.afk")),
                value = yesNo(live.afk),
            )
        )
        live.address?.let { address ->
            add(
                DetailField(
                    label = LocalizedText(MessageKey("player.whois.field.ip")),
                    value = text(address),
                )
            )
        }
    }

private fun PlayerMessages.yesNo(value: Boolean): LocalizedText =
    LocalizedText(
        when {
            value -> MessageKey("player.whois.value.yes")
            else -> MessageKey("player.whois.value.no")
        }
    )

private fun PlayerMessages.agoOrUnknown(at: Instant?, now: Instant): LocalizedText =
    when (at) {
        null -> LocalizedText(MessageKey("player.whois.value.unknown"))
        else -> LocalizedText(
            MessageKey("player.whois.value.ago"),
            listOf(elapsed(since(at, now)))
        )
    }
