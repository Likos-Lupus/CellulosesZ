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

/** `/ptime`. */
internal object PersonalTimeCommands {

    private val PERSONAL_TIME_SUGGESTIONS: List<String> =
        TIME_SUGGESTIONS + TIME_SUGGESTIONS.map { "@$it" }

    fun commands(
        service: WorldToolsService,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        personalTimeCommand(service, personal, adventure, presentation),
    )

    // ---------------------------------------------------------------- /ptime

    private fun personalTimeCommand(
        service: WorldToolsService,
        personal: PersonalViews,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "ptime",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.P_TIME,
            documentation = "administration/ptime",
        ) {
            executesPlayer {
                listPersonalTime(
                    source,
                    personal,
                    adventure,
                    presentation
                )
            }
            literal("list") {
                executesPlayer {
                    listPersonalTime(
                        source,
                        personal,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.P_TIME_OTHERS
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
                        listPersonalTime(
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
                    resetPersonalTime(
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
                    permission = CommandPermissions.P_TIME_OTHERS
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
                        resetPersonalTime(
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
                suggests { suggestAll(PERSONAL_TIME_SUGGESTIONS) }
                executesPlayer {
                    setPersonalTime(
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
                    permission = CommandPermissions.P_TIME_OTHERS
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
                        setPersonalTime(
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

    private fun listPersonalTime(
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
        val notice = when (val view = personal.time(player.uuid)) {
            null -> WorldToolsMessages.personalTimeNone()
            else -> WorldToolsMessages.personalTimeList(formatPersonalTime(view))
        }
        return reply(
            source,
            adventure,
            presentation,
            notice
        )
    }

    private fun setPersonalTime(
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
        val view = parsePersonalTime(raw)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.timeInvalid()
            )
        personal.setTime(player.uuid, view)
        service.sendPersonalTime(player, view)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalTimeSet(formatPersonalTime(view)),
        )
    }

    private fun resetPersonalTime(
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
        personal.clearTime(player.uuid)
        service.currentTime(player.level())?.let {
            service.sendPersonalTime(
                player,
                PersonalTimeView(it, false)
            )
        }
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.personalTimeReset()
        )
    }

    private fun parsePersonalTime(raw: String): PersonalTimeView? {
        val frozen = raw.startsWith("@")
        val ticks = TimeValueParser.parse(if (frozen) raw.substring(1) else raw)
            ?: return null
        return PersonalTimeView(ticks, frozen)
    }

    private fun formatPersonalTime(view: PersonalTimeView): String =
        when {
            view.frozen -> "${view.ticks} ticks (fixed)"
            else -> "${view.ticks} ticks"
        }

}
