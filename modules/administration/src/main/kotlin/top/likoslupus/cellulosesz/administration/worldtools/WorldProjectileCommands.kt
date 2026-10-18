package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.config.WorldToolsSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.permission.VanillaPermissionFallback
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/fireball`, `/firework` and `/nuke`. */
internal object WorldProjectileCommands {

    private val FIREBALL_SUGGESTIONS: List<String> = listOf(
        "fireball",
        "small",
        "large",
        "dragon",
        "skull",
        "arrow",
        "egg",
        "snowball",
        "expbottle",
        "splashpotion",
        "lingeringpotion",
        "trident",
        "windcharge",
    )

    fun commands(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        permissions: PermissionService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        fireballCommand(service, settings, permissions, adventure, presentation),
        fireworkCommand(service, settings, adventure, presentation),
        nukeCommand(service, adventure, presentation),
    )

    // ---------------------------------------------------------------- /fireball

    private fun fireballCommand(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        permissions: PermissionService,
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
                    "fireball",
                    2.0,
                    false,
                    settings,
                    permissions,
                    service,
                    adventure,
                    presentation,
                )
            }
            argument("type", word()) { type ->
                suggests {
                    suggestAll(
                        FIREBALL_SUGGESTIONS.filter { candidate ->
                            FireballType.parse(candidate)?.let { parsed ->
                                permissions.allows(source, fireballPermission(parsed))
                            } ?: false
                        }
                    )
                }
                executesPlayer {
                    launchFireball(
                        source,
                        get(type),
                        2.0,
                        false,
                        settings,
                        permissions,
                        service,
                        adventure,
                        presentation,
                    )
                }
                argument("speed", double(0.0, 64.0)) { speed ->
                    executesPlayer {
                        launchFireball(
                            source,
                            get(type),
                            get(speed),
                            false,
                            settings,
                            permissions,
                            service,
                            adventure,
                            presentation,
                        )
                    }
                    literal("ride", permission = CommandPermissions.FIREBALL_RIDE) {
                        executesPlayer {
                            launchFireball(
                                source,
                                get(type),
                                get(speed),
                                true,
                                settings,
                                permissions,
                                service,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
        }

    private fun launchFireball(
        source: CommandSourceStack,
        rawType: String,
        speed: Double,
        ride: Boolean,
        settings: () -> WorldToolsSettings,
        permissions: PermissionService,
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
        if (!permissions.allows(source, fireballPermission(type))) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.fireballForbidden(type)
            )
        }
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        val capped = minOf(speed, settings().maxProjectileSpeed)
        service.launchFireball(player, type, capped, ride)
        return reply(
            source,
            adventure,
            presentation,
            WorldToolsMessages.fireballLaunched(type)
        )
    }

    private fun fireballPermission(type: FireballType): PermissionSpec =
        PermissionSpec(
            "cellulosesz.fireball.${type.name.lowercase()}",
            VanillaPermissionFallback.COMMANDS_MODERATOR,
            "Use /fireball ${type.name.lowercase()}",
        )

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
            literal("fire", permission = CommandPermissions.FIREWORK_FIRE) {
                executesPlayer {
                    val launched = service.launchFireworks(source.player!!, 1, 1)
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.fireworkFired(launched)
                    )
                }
                argument("amountOrDirection", word()) { target ->
                    suggests {
                        suggestAll((1..settings().maxFireworkAmount).map(Int::toString))
                    }
                    executesPlayer {
                        val raw = get(target)
                        val amount = raw.toIntOrNull()
                        val direction = amount == null
                        val capped = when {
                            direction -> 1
                            else -> minOf(amount, settings().maxFireworkAmount)
                        }
                        val launched = service.launchFireworks(
                            source.player!!,
                            1,
                            capped,
                            direction,
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
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "nuke",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.NUKE,
            documentation = "administration/nuke",
        ) {
            executes {
                val source = context.source
                val targets = source.server.playerList.players.toList()
                if (targets.isEmpty()) {
                    return@executes reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.playerNotFound(),
                    )
                }
                targets.forEach { target ->
                    service.nuke(target.level(), target.x, target.z)
                }
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.nukeDetonated(),
                )
            }
            argument("players", greedyString()) { players ->
                executes {
                    val source = context.source
                    val targets = get(players)
                            .split(' ')
                            .filter { it.isNotBlank() }
                            .mapNotNull(source.server.playerList::getPlayerByName)
                    if (targets.isEmpty()) {
                        return@executes reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.playerNotFound(),
                        )
                    }
                    targets.forEach { target ->
                        service.nuke(target.level(), target.x, target.z)
                    }
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.nukeDetonated(),
                    )
                }
            }
        }

}
