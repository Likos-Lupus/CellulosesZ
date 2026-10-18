package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/pweather`. */
internal object PersonalWeatherCommands {

    fun commands(
        service: WorldToolsService,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        personalWeatherCommand(service, personal, adventure, presentation),
    )

    // ---------------------------------------------------------------- /pweather

    private fun personalWeatherCommand(
        service: WorldToolsService,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "pweather",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.P_WEATHER,
            documentation = "administration/pweather",
        ) {
            executesPlayer {
                listPersonalWeather(
                    source,
                    personal,
                    adventure,
                    presentation
                )
            }
            literal("list") {
                executesPlayer {
                    listPersonalWeather(
                        source,
                        personal,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_WEATHER_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        listPersonalWeather(
                            source,
                            personal,
                            adventure,
                            presentation,
                            player
                        )
                    }
                }
            }
            literal("reset") {
                executesPlayer {
                    resetPersonalWeather(
                        source,
                        personal,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_WEATHER_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        resetPersonalWeather(
                            source,
                            personal,
                            service,
                            adventure,
                            presentation,
                            player
                        )
                    }
                }
            }
            argument("value", word()) { value ->
                suggests { suggestAll(WEATHER_SUGGESTIONS) }
                executesPlayer {
                    setPersonalWeather(
                        source,
                        personal,
                        service,
                        get(value),
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_WEATHER_OTHERS
                ) { target ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        setPersonalWeather(
                            source,
                            personal,
                            service,
                            get(value),
                            adventure,
                            presentation,
                            player,
                        )
                    }
                }
            }
        }

    private fun listPersonalWeather(
        source: CommandSourceStack,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        val notice = when (val weather = personal.weather(player.uuid)) {
            null -> WorldToolsMessages.personalWeatherNone()
            else -> WorldToolsMessages.personalWeatherList(weather.name.lowercase())
        }
        return reply(source, adventure, presentation, notice)
    }

    private fun setPersonalWeather(
        source: CommandSourceStack,
        personal: PersonalViews,
        service: WorldToolsService,
        raw: String,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        val weather = PersonalWeather.parse(raw)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.weatherInvalid()
            )
        personal.setWeather(player.uuid, weather)
        service.sendPersonalWeather(player, weather)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalWeatherSet(weather.name.lowercase()),
        )
    }

    private fun resetPersonalWeather(
        source: CommandSourceStack,
        personal: PersonalViews,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        player: ServerPlayer? = source.player,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        personal.clearWeather(player.uuid)
        val level = player.level()
        val weather = when {
            level.isThundering -> PersonalWeather.THUNDER
            level.isRaining -> PersonalWeather.RAIN
            else -> PersonalWeather.CLEAR
        }
        service.sendPersonalWeather(player, weather)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalWeatherReset()
        )
    }

}
