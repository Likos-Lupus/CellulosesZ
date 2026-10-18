package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.administration.config.WorldToolsSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.double
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/fireball`, `/firework` and `/nuke`. */
internal object WorldProjectileCommands {

    private val FIREBALL_SUGGESTIONS: List<String> = listOf(
        "small",
        "large",
        "dragon",
        "wither"
    )

    fun commands(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        fireballCommand(service, adventure, presentation),
        fireworkCommand(service, settings, adventure, presentation),
        nukeCommand(service, settings, adventure, presentation),
    )

    // ---------------------------------------------------------------- /fireball

    private fun fireballCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "fireball",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FIREBALL,
            documentation = "administration/fireball",
        ) {
            executesPlayer {
                launchFireball(
                    source,
                    "large",
                    1.0,
                    service,
                    adventure,
                    presentation
                )
            }
            argument("type", word()) { type ->
                suggests { suggestAll(FIREBALL_SUGGESTIONS) }
                executesPlayer {
                    launchFireball(
                        source,
                        get(type),
                        1.0,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("speed", double(0.1, 10.0)) { speed ->
                    executesPlayer {
                        launchFireball(
                            source,
                            get(type),
                            get(speed),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        }

    private fun launchFireball(
        source: CommandSourceStack,
        rawType: String,
        speed: Double,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val type = FireballType.parse(rawType)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.fireballInvalid()
            )
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        service.launchFireball(player, type, speed)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.fireballLaunched(type)
        )
    }

    // ---------------------------------------------------------------- /firework

    private fun fireworkCommand(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "firework",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FIREWORK,
            documentation = "administration/firework",
        ) {
            argument("power", integer(1, 32)) { power ->
                executesPlayer {
                    val capped = minOf(get(power), settings().maxFireworkPower)
                    if (service.setFireworkPower(source.player!!, capped)) {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkPower(capped)
                        )
                    } else {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkNoRocket()
                        )
                    }
                }
            }
            literal("clear") {
                executesPlayer {
                    if (service.clearFirework(source.player!!)) {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkCleared()
                        )
                    } else {
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkNoRocket()
                        )
                    }
                }
            }
            literal("fire") {
                executesPlayer {
                    val launched = service.launchFireworks(source.player!!, 1, 1)
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.fireworkFired(launched)
                    )
                }
                argument("amount", integer(1, 128)) { amount ->
                    executesPlayer {
                        val capped = minOf(get(amount), settings().maxFireworkAmount)
                        val launched = service.launchFireworks(
                            source.player!!,
                            1,
                            capped
                        )
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.fireworkFired(launched)
                        )
                    }
                }
            }
        }

    // ---------------------------------------------------------------- /nuke

    private fun nukeCommand(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "nuke",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.NUKE,
            documentation = "administration/nuke",
        ) {
            executesPlayer {
                detonate(
                    source,
                    source.player,
                    service,
                    settings,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { target ->
                executes {
                    val source = context.source
                    detonate(
                        source,
                        resolveTarget(source, get(target)),
                        service,
                        settings,
                        adventure,
                        presentation,
                    )
                }
            }
        }

    private fun detonate(
        source: CommandSourceStack,
        player: ServerPlayer?,
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        }
        service.nuke(
            player.level(),
            player.x,
            player.y,
            player.z,
            settings().maxNukeRadius
        )
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.nukeDetonated()
        )
    }

}
