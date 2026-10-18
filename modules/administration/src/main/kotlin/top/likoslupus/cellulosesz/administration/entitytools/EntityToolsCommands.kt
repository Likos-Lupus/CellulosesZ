package top.likoslupus.cellulosesz.administration.entitytools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import top.likoslupus.cellulosesz.administration.config.EntityToolsSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice

/** `/spawner`, `/spawnmob` and `/remove`. */
internal object EntityToolsCommands {

    private const val REACH: Double = 8.0
    private const val MAX_DELAY_TICKS: Int = 72_000

    fun commands(
        service: EntityToolsService,
        settings: () -> EntityToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        spawnerCommand(service, adventure, presentation),
        spawnMobCommand(service, settings, adventure, presentation),
        removeCommand(service, settings, adventure, presentation),
    )

    private fun reply(
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

    private fun spawnerCommand(
        service: EntityToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "spawner",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.SPAWNER,
            documentation = "administration/spawner",
        ) {
            argument("entity", word()) { entity ->
                suggests { suggestAll(service.knownEntityTypeIds()) }
                executesPlayer {
                    applySpawner(
                        source,
                        get(entity),
                        null,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "delay",
                    integer(1, MAX_DELAY_TICKS),
                    permission = CommandPermissions.SPAWNER_DELAY,
                ) { delay ->
                    executesPlayer {
                        applySpawner(
                            source,
                            get(entity),
                            get(delay),
                            service,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        }

    private fun applySpawner(
        source: CommandSourceStack,
        rawType: String,
        delayTicks: Int?,
        service: EntityToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val type = service.resolveEntityType(rawType)
            ?: return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.unknownEntityType(rawType)
            )
        val pos = lookedAtBlock(source)
            ?: return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.lookAtBlock()
            )
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.playerNotFound()
            )
        if (
            !service.configureSpawner(
                player.level(),
                pos,
                type,
                delayTicks
            )
        ) {
            return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.notASpawner()
            )
        }
        val displayId = Identifier.tryParse(
            if (rawType.contains(':')) rawType else "minecraft:$rawType"
        )?.toString() ?: rawType
        return reply(
            source,
            adventure,
            presentation,
            EntityToolsMessages.spawnerConfigured(displayId, delayTicks),
        )
    }

    private fun spawnMobCommand(
        service: EntityToolsService,
        settings: () -> EntityToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "spawnmob",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.SPAWN_MOB,
            documentation = "administration/spawnmob",
        ) {
            argument("entity", word()) { entity ->
                suggests { suggestAll(service.knownEntityTypeIds()) }
                executesPlayer {
                    spawnMob(
                        source,
                        source.player,
                        get(entity),
                        1,
                        service,
                        settings,
                        adventure,
                        presentation
                    )
                }
                argument("amount", integer(1, 1_024)) { amount ->
                    executesPlayer {
                        spawnMob(
                            source,
                            source.player,
                            get(entity),
                            get(amount),
                            service,
                            settings,
                            adventure,
                            presentation,
                        )
                    }
                    argument("player", word()) { target ->
                        executes {
                            val source = context.source
                            val player = resolveTarget(source, get(target))
                                ?: return@executes reply(
                                    source,
                                    adventure,
                                    presentation,
                                    EntityToolsMessages.playerNotFound(),
                                )
                            spawnMob(
                                source,
                                player,
                                get(entity),
                                get(amount),
                                service,
                                settings,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
        }

    private fun spawnMob(
        source: CommandSourceStack,
        player: ServerPlayer?,
        rawType: String,
        amount: Int,
        service: EntityToolsService,
        settings: () -> EntityToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        if (player == null) {
            return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.playerNotFound()
            )
        }
        val type = service.resolveEntityType(rawType)
            ?: return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.unknownEntityType(rawType)
            )
        val capped = minOf(amount, settings().maxSpawnAmount)
        val spawned = service.spawnMobs(
            player.level(),
            type,
            player.x,
            player.y,
            player.z,
            player.yRot,
            player.xRot,
            capped,
        )
        val notice = when {
            capped < amount ->
                EntityToolsMessages.spawnMobCapped(
                    amount,
                    spawned
                )
            else ->
                EntityToolsMessages.spawnMobSpawned(
                    rawType.lowercase(),
                    spawned
                )
        }
        return reply(
            source,
            adventure,
            presentation,
            notice
        )
    }

    private fun removeCommand(
        service: EntityToolsService,
        settings: () -> EntityToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "remove",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.REMOVE,
            documentation = "administration/remove",
        ) {
            argument("selector", word()) { selector ->
                suggests {
                    suggestAll(EntitySelector.tokens)
                    suggestAll(
                        BuiltInRegistries.ENTITY_TYPE.keySet()
                                .take(256)
                                .map(Identifier::toString)
                    )
                }
                executesPlayer {
                    remove(
                        source,
                        get(selector),
                        null,
                        service,
                        settings,
                        adventure,
                        presentation
                    )
                }
                argument("scope", word()) { scope ->
                    executesPlayer {
                        remove(
                            source,
                            get(selector),
                            get(scope),
                            service,
                            settings,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        }

    private fun remove(
        source: CommandSourceStack,
        rawSelector: String,
        scope: String?,
        service: EntityToolsService,
        settings: () -> EntityToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val target = RemoveSelectorParser.parse(rawSelector)
            ?: return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.removeInvalid()
            )
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.playerNotFound()
            )
        val radius = when {
            scope == null -> null
            scope.equals("world", ignoreCase = true) -> null
            scope.equals("all", ignoreCase = true) -> null
            else -> scope.toDoubleOrNull()
                ?: return reply(
                    source,
                    adventure,
                    presentation,
                    EntityToolsMessages.invalidRadius()
                )
        }
        val max = settings().maxRemoveRadius
        if (radius != null && (radius <= 0.0 || radius > max)) {
            return reply(
                source,
                adventure,
                presentation,
                EntityToolsMessages.radiusTooLarge(max)
            )
        }
        val removed = service.remove(
            player.level(),
            target,
            player.x,
            player.y,
            player.z,
            radius,
        )
        return reply(
            source,
            adventure,
            presentation,
            EntityToolsMessages.removeSuccess(rawSelector, removed)
        )
    }

    private fun resolveTarget(source: CommandSourceStack, name: String?): ServerPlayer? =
        when (name) {
            null -> source.player
            else -> PlayerResolver.onlineByName(source.server, name)
        }

    private fun lookedAtBlock(source: CommandSourceStack): BlockPos? {
        val player = source.player
            ?: return null
        val hit: HitResult = player.pick(REACH, 0f, false)
        return (hit as? BlockHitResult)?.blockPos
    }

}
