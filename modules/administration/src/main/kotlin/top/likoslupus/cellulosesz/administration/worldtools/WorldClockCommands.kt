package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/time`, `/weather`, `/thunder` and `/lightning`. */
internal object WorldClockCommands {

    fun commands(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        timeCommand(service, adventure, presentation),
        weatherCommand(service, adventure, presentation),
        thunderCommand(service, adventure, presentation),
        lightningCommand(service, adventure, presentation),
    )

    // ---------------------------------------------------------------- helpers

    private fun resolveLevels(
        source: CommandSourceStack,
        scope: String?
    ): List<ServerLevel> =
        when {
            scope == null -> listOf(source.level)
            scope.equals("all", ignoreCase = true) -> source.server.allLevels.toList()
            else -> source.server.allLevels.filter {
                val id = it.dimension().identifier()
                id.toString() == scope || id.path == scope
            }
        }

    // ---------------------------------------------------------------- /time

    private fun timeCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "time",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TIME,
            documentation = "administration/time",
            aliases = setOf("day", "night"),
        ) {
            executes {
                val source = context.source
                val label = context.input.trim().substringBefore(' ').lowercase()
                when (label) {
                    "day" -> return@executes setTime(
                        source,
                        "day",
                        null,
                        false,
                        service,
                        adventure,
                        presentation,
                    )

                    "night" -> return@executes setTime(
                        source,
                        "night",
                        null,
                        false,
                        service,
                        adventure,
                        presentation,
                    )
                }
                when (val ticks = service.currentTime(source.level)) {
                    null -> reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.timeUnavailable()
                    )

                    else -> reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.timeCurrent(ticks)
                    )
                }
            }
            literal("set") {
                argument("value", word()) { value ->
                    suggests { suggestAll(TIME_SUGGESTIONS) }
                    executes {
                        setTime(
                            context.source,
                            get(value),
                            null,
                            false,
                            service,
                            adventure,
                            presentation
                        )
                    }
                    argument("scope", word()) { scope ->
                        executes {
                            setTime(
                                context.source,
                                get(value),
                                get(scope),
                                false,
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
            literal("add") {
                argument("value", word()) { value ->
                    suggests { suggestAll(TIME_SUGGESTIONS) }
                    executes {
                        setTime(
                            context.source,
                            get(value),
                            null,
                            true,
                            service,
                            adventure,
                            presentation
                        )
                    }
                    argument("scope", word()) { scope ->
                        executes {
                            setTime(
                                context.source,
                                get(value),
                                get(scope),
                                true,
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
        }

    private fun setTime(
        source: CommandSourceStack,
        rawValue: String,
        scope: String?,
        add: Boolean,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val ticks = TimeValueParser.parse(rawValue)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.timeInvalid()
            )
        val targets = resolveLevels(source, scope)
        if (targets.isEmpty()) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.timeUnavailable()
            )
        }
        targets.forEach {
            when {
                add -> service.addTime(it, ticks)
                else -> service.setTime(it, ticks)
            }
        }
        val notice = when {
            add -> WorldToolsMessages.timeAdded(ticks)
            else -> WorldToolsMessages.timeSet(ticks)
        }
        return reply(source, adventure, presentation, notice)
    }

    // ---------------------------------------------------------------- /weather

    private fun weatherCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "weather",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.WEATHER,
            documentation = "administration/weather",
        ) {
            argument("type", word()) { type ->
                suggests { suggestAll(WEATHER_SUGGESTIONS) }
                executes {
                    applyWeather(
                        context.source,
                        get(type),
                        null,
                        null,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("duration", integer(1, 1_000_000)) { duration ->
                    executes {
                        applyWeather(
                            context.source,
                            get(type),
                            get(duration).toLong(),
                            null,
                            service,
                            adventure,
                            presentation,
                        )
                    }
                    argument("scope", word()) { scope ->
                        executes {
                            applyWeather(
                                context.source,
                                get(type),
                                get(duration).toLong(),
                                get(scope),
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
        }

    private fun applyWeather(
        source: CommandSourceStack,
        rawType: String,
        seconds: Long?,
        scope: String?,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val normalized = when (rawType.lowercase()) {
            "storm" -> "rain"
            "sun" -> "clear"
            else -> rawType
        }
        val type = WeatherType.parse(normalized)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.weatherInvalid()
            )
        val durationTicks = seconds?.let { (it * 20L).toInt() }
        resolveLevels(source, scope).forEach {
            service.setWeather(it, type, durationTicks)
        }
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.weatherSet(type, seconds)
        )
    }

    // ---------------------------------------------------------------- /thunder

    private fun thunderCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "thunder",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.THUNDER,
            documentation = "administration/thunder",
        ) {
            literal("true") {
                executes {
                    val source = context.source
                    resolveLevels(source, null).forEach {
                        service.setThunder(it, true, null)
                    }
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.thunderSet(true)
                    )
                }
                argument("duration", integer(1, 1_000_000)) { duration ->
                    executes {
                        val source = context.source
                        val ticks = get(duration) * 20
                        resolveLevels(source, null).forEach {
                            service.setThunder(it, true, ticks)
                        }
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.thunderSet(true)
                        )
                    }
                }
            }
            literal("false") {
                executes {
                    val source = context.source
                    resolveLevels(source, null).forEach {
                        service.setThunder(it, false, null)
                    }
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.thunderSet(false)
                    )
                }
            }
        }

    // ---------------------------------------------------------------- /lightning

    private const val LIGHTNING_LOOK_REACH: Double = 600.0
    private const val DEFAULT_LIGHTNING_DAMAGE: Int = 5

    private fun lightningCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "lightning",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.LIGHTNING,
            documentation = "administration/lightning",
        ) {
            executesPlayer {
                val player = source.player
                    ?: return@executesPlayer reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.playerNotFound(),
                    )
                val pos = blockPos(player.pick(LIGHTNING_LOOK_REACH, 0f, false))
                    ?: return@executesPlayer reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.lookAtBlock(),
                    )
                service.strikeLightning(
                    player.level(),
                    pos.x + 0.5,
                    pos.y.toDouble(),
                    pos.z + 0.5,
                    false,
                )
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.lightningStruck(),
                )
            }
            argument("player", word(), permission = CommandPermissions.LIGHTNING_OTHERS) { target ->
                executes {
                    val source = context.source
                    val player = resolveTarget(source, get(target))
                        ?: return@executes reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.playerNotFound(),
                        )
                    strikeTarget(
                        source,
                        player,
                        DEFAULT_LIGHTNING_DAMAGE,
                        service,
                        adventure,
                        presentation,
                    )
                }
                argument("power", integer(1, 100)) { power ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        strikeTarget(
                            source,
                            player,
                            get(power),
                            service,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        }

    private fun strikeTarget(
        source: CommandSourceStack,
        player: ServerPlayer,
        power: Int,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        service.strikeLightning(
            player.level(),
            player.x,
            player.y,
            player.z,
            true,
        )
        service.smite(player, power)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.lightningSmitten(player.gameProfile.name, power),
        )
    }

    private fun blockPos(hit: HitResult?): BlockPos? =
        (hit as? BlockHitResult)?.blockPos

}
