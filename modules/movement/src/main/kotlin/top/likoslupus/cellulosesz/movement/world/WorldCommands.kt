package top.likoslupus.cellulosesz.movement.world

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyDetail
import top.likoslupus.cellulosesz.core.text.presentation.replyPage
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.MovementSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.teleport.*

/**
 * `/world [name]` and `/world list`. Resolution is alias-or-id; a teleport always goes through the
 * single [TeleportCoordinator] path, so world-access, safety, delay and cooldown rules stay uniform.
 */
internal object WorldCommands {

    fun commands(
        worlds: WorldSource,
        teleports: TeleportCoordinator,
        settings: () -> MovementSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "world",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.WORLD,
            documentation = "movement/world",
        ) {
            executesPlayer {
                current(
                    source,
                    settings,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("name", word()) { name ->
                suggests {
                    suggestAll(settings().worlds.aliases.keys)
                    suggestAll(loadedDimensions(source))
                }
                executesPlayer {
                    travel(
                        context,
                        get(name),
                        worlds,
                        teleports,
                        settings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("list") {
                executes {
                    list(
                        source,
                        settings,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun current(
        source: CommandSourceStack,
        settings: () -> MovementSettings,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val playerId = player.uuid
        val dimension = player.level().dimension().identifier().toString()
        val directory = WorldDirectory(
            settings().worlds.aliases
        )
        return source.replyDetail(
            adventure,
            presentation,
            playerId,
            MovementMessages.currentWorld(
                directory.aliasOf(dimension),
                dimension,
            ),
        )
    }

    private fun list(
        source: CommandSourceStack,
        settings: () -> MovementSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val directory = WorldDirectory(
            settings().worlds.aliases
        )
        return source.replyPage(
            adventure,
            presentation,
            source.player?.uuid,
            MovementMessages.worlds(
                directory.entries(loadedDimensions(source).toSet())
            ),
        )
    }

    private fun travel(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        worlds: WorldSource,
        teleports: TeleportCoordinator,
        settings: () -> MovementSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val directory = WorldDirectory(settings().worlds.aliases)

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport"),
        ) { target ->
            val available = worlds.availableDimensions()
            val dimension = directory.resolve(rawName, available.toSet())
            val notices = when {
                dimension == null -> listOf(MovementMessages.unknownWorld(rawName))
                else -> {
                    val spawn = worlds.spawnPosition(dimension)
                    when {
                        spawn == null -> listOf(MovementMessages.unknownWorld(dimension))
                        else -> {
                            val intent = TeleportIntent(
                                subjectId = playerId,
                                destination = TeleportDestination.Fixed(spawn),
                                cause = TeleportCause.SPAWN,
                                policy = teleportPolicyFor(
                                    TeleportCause.SPAWN,
                                    settings().teleport,
                                ),
                            )
                            when (val outcome = teleports.execute(intent)) {
                                is TeleportOutcome.Success -> listOf(
                                    MovementMessages.teleportedToWorld(
                                        directory.aliasOf(dimension) ?: dimension
                                    )
                                )

                                else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
                            }
                        }
                    }
                }
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                notices,
            )
        }
    }

    private fun loadedDimensions(source: CommandSourceStack): List<String> =
        source.server.allLevels.map { it.dimension().identifier().toString() }

}
