package top.likoslupus.cellulosesz.administration.worldtools

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.core.text.presentation.MessageSupport
import java.util.*

/** Semantic results for the world/environment tool commands. */
internal object WorldToolsMessages : MessageSupport() {

    fun timeSet(ticks: Long): Notice =
        success(
            MessageKey("administration.time.set"),
            count(ticks),
        )

    fun timeAdded(ticks: Long): Notice =
        success(
            MessageKey("administration.time.added"),
            count(ticks),
        )

    fun timeForAll(): Notice =
        info(MessageKey("administration.time.all"))

    fun timeUnavailable(): Notice =
        error(MessageKey("administration.time.no_clock"))

    fun timeCurrent(ticks: Long): Notice =
        info(
            MessageKey("administration.time.current"),
            count(ticks),
        )

    fun timeInvalid(): Notice =
        error(MessageKey("administration.time.invalid"))

    fun weatherSet(type: WeatherType, seconds: Long?): Notice =
        when (seconds) {
            null -> success(
                MessageKey("administration.weather.set"),
                literal(type.name.lowercase()),
            )

            else -> success(
                MessageKey("administration.weather.set_duration"),
                literal(type.name.lowercase()),
                count(seconds),
            )
        }

    fun weatherInvalid(): Notice =
        error(MessageKey("administration.weather.invalid"))

    fun thunderSet(enabled: Boolean): Notice =
        success(
            MessageKey("administration.thunder.set"),
            literal(enabled.toString()),
        )

    fun lightningStruck(count: Int): Notice =
        success(
            MessageKey("administration.lightning.struck"),
            count(count),
        )

    fun fireballLaunched(type: FireballType): Notice =
        success(
            MessageKey("administration.fireball.launched"),
            literal(type.name.lowercase()),
        )

    fun fireballInvalid(): Notice =
        error(MessageKey("administration.fireball.invalid"))

    fun fireworkPower(power: Int): Notice =
        success(
            MessageKey("administration.firework.power"),
            count(power),
        )

    fun fireworkCleared(): Notice =
        success(MessageKey("administration.firework.cleared"))

    fun fireworkFired(count: Int): Notice =
        success(
            MessageKey("administration.firework.fired"),
            count(count),
        )

    fun fireworkNoRocket(): Notice =
        error(MessageKey("administration.firework.no_rocket"))

    fun nukeDetonated(): Notice =
        success(MessageKey("administration.nuke.detonated"))

    fun treeGrown(type: String): Notice =
        success(
            MessageKey("administration.tree.grown"),
            literal(type),
        )

    fun treeFailed(): Notice =
        error(MessageKey("administration.tree.failed"))

    fun treeInvalid(): Notice =
        error(MessageKey("administration.tree.invalid"))

    fun brokeBlock(): Notice =
        success(MessageKey("administration.break.success"))

    fun lookAtBlock(): Notice =
        error(MessageKey("administration.tools.look_at_block"))

    fun iceFrozen(count: Int): Notice =
        success(
            MessageKey("administration.ice.frozen"),
            count(count),
        )

    fun extinguished(): Notice =
        success(MessageKey("administration.ext.success"))

    fun burned(target: String, seconds: Double): Notice =
        success(
            MessageKey("administration.burn.success"),
            playerName(target),
            literal(format(seconds)),
        )

    fun playerNotFound(): Notice =
        error(MessageKey("administration.playerstate.player_not_found"))

    fun personalTimeSet(value: String): Notice =
        success(
            MessageKey("administration.ptime.set"),
            literal(value),
        )

    fun personalTimeReset(): Notice =
        success(MessageKey("administration.ptime.reset"))

    fun personalTimeList(value: String): Notice =
        info(
            MessageKey("administration.ptime.list"),
            literal(value),
        )

    fun personalTimeNone(): Notice =
        info(MessageKey("administration.ptime.none"))

    fun personalWeatherSet(value: String): Notice =
        success(
            MessageKey("administration.pweather.set"),
            literal(value),
        )

    fun personalWeatherReset(): Notice =
        success(MessageKey("administration.pweather.reset"))

    fun personalWeatherList(value: String): Notice =
        info(
            MessageKey("administration.pweather.list"),
            literal(value),
        )

    fun personalWeatherNone(): Notice =
        info(MessageKey("administration.pweather.none"))

    fun stats(stats: ServerStats): DetailView =
        DetailView(
            title = LocalizedText(MessageKey("administration.gc.title")),
            fields = buildList {
                add(
                    field(
                        MessageKey("administration.gc.uptime"),
                        LocalizedText(
                            MessageKey("administration.gc.seconds"),
                            listOf(count(stats.uptimeTicks / 20)),
                        ),
                    )
                )
                add(
                    field(
                        MessageKey("administration.gc.tps"),
                        LocalizedText(
                            MessageKey("core.value"),
                            listOf(literal(format(stats.tps))),
                        ),
                    )
                )
                add(
                    field(
                        MessageKey("administration.gc.tick"),
                        LocalizedText(
                            MessageKey("administration.gc.millis"),
                            listOf(literal(format(stats.averageTickMillis))),
                        ),
                    )
                )
                add(
                    field(
                        MessageKey("administration.gc.memory"),
                        LocalizedText(
                            MessageKey("administration.gc.memory_value"),
                            listOf(
                                literal(formatMegabytes(stats.usedMemoryBytes)),
                                literal(formatMegabytes(stats.totalMemoryBytes)),
                                literal(formatMegabytes(stats.maxMemoryBytes)),
                            ),
                        ),
                    )
                )
                stats.worlds.forEach { world ->
                    add(
                        field(
                            MessageKey("core.value"),
                            LocalizedText(
                                MessageKey("administration.gc.world_row"),
                                listOf(
                                    identifier(world.dimension),
                                    count(world.players),
                                    count(world.chunks),
                                    count(world.entities),
                                ),
                            ),
                        )
                    )
                }
            },
        )

    private fun format(value: Double): String =
        String.format(
            Locale.ROOT,
            "%.2f",
            value
        )

    private fun formatMegabytes(bytes: Long): String =
        String.format(
            Locale.ROOT,
            "%.1f",
            bytes.toDouble() / (1024.0 * 1024.0)
        )

}
