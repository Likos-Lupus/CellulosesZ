package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice

/** Replies to [source] with a semantic [notice] rendered for that recipient. */
internal fun reply(
    source: CommandSourceStack,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    notice: Notice,
): Int =
    source.replyNotice(
        adventure,
        presentation,
        source.player?.uuid,
        notice
    )

/** Resolves [name] to an online player, or the executing player when [name] is absent. */
internal fun resolveTarget(
    source: CommandSourceStack,
    name: String?
): ServerPlayer? =
    if (name == null) {
        source.player
    } else {
        PlayerResolver.onlineByName(source.server, name)
    }

/** Shared `/time` and `/ptime` value completions. */
internal val TIME_SUGGESTIONS: List<String> = listOf(
    "day",
    "noon",
    "night",
    "midnight",
    "dawn",
    "dusk",
    "17:30",
    "4pm",
    "4000ticks",
)

/** Shared `/weather` and `/pweather` value completions. */
internal val WEATHER_SUGGESTIONS: List<String> = listOf(
    "clear",
    "rain",
    "thunder",
    "storm",
    "sun"
)
