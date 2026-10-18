package top.likoslupus.cellulosesz.movement.random

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.double
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendDetail
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.core.text.presentation.sendPage
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.RandomTeleportSettings
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.*
import top.likoslupus.cellulosesz.movement.teleport.*

internal object RandomTeleportCommands {

    fun commands(
        service: RandomTeleportService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        randomSettings: () -> RandomTeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tpr",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TPR,
            documentation = "movement/tpr",
        ) {
            executesPlayer {
                teleportRandom(
                    context,
                    null,
                    service,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("region", word()) { region ->
                executesPlayer {
                    teleportRandom(
                        context,
                        get(region),
                        service,
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "settpr",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.SET_TPR,
            documentation = "movement/settpr",
        ) {
            literal("list") {
                executes {
                    listRegions(
                        source,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("info") {
                argument("region", word()) { region ->
                    executes {
                        infoRegion(
                            source,
                            get(region),
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
            literal("create") {
                argument("region", word()) { region ->
                    argument("minRadius", double(0.0)) { minRadius ->
                        argument("maxRadius", double(0.0)) { maxRadius ->
                            executesPlayer {
                                createRegion(
                                    context,
                                    get(region),
                                    get(minRadius),
                                    get(maxRadius),
                                    service,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation,
                                )
                            }
                        }
                    }
                }
            }
            literal("center") {
                argument("region", word()) { region ->
                    executesPlayer {
                        centerRegion(
                            context,
                            get(region),
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
            literal("range") {
                argument("region", word()) { region ->
                    argument("minRadius", double(0.0)) { minRadius ->
                        argument("maxRadius", double(0.0)) { maxRadius ->
                            executes {
                                rangeRegion(
                                    source,
                                    get(region),
                                    get(minRadius),
                                    get(maxRadius),
                                    service,
                                    kernel,
                                    messages,
                                    adventure,
                                    presentation,
                                )
                            }
                        }
                    }
                }
            }
            literal("remove") {
                argument("region", word()) { region ->
                    executes {
                        removeRegion(
                            source,
                            get(region),
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        },
    )

    private fun teleportRandom(
        context: CommandContext<CommandSourceStack>,
        regionName: String?,
        service: RandomTeleportService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                ),
            )
        val playerId = player.uuid
        val dimension = player.toStoredPosition().dimension

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.random_teleport"),
        ) { target ->
            val notices = when (val result = service.roll(playerId, dimension, regionName)) {
                is RandomTeleportResult.Found -> when (
                    val outcome = teleports.execute(
                        TeleportIntent(
                            subjectId = playerId,
                            destination = TeleportDestination.Fixed(result.position),
                            cause = TeleportCause.RANDOM,
                            policy = teleportPolicyFor(
                                TeleportCause.RANDOM,
                                teleportSettings(),
                            ),
                        )
                    )
                ) {
                    is TeleportOutcome.Success -> listOf(MovementMessages.randomTeleported())
                    else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
                }

                else -> listOfNotNull(MovementMessages.randomTeleportError(result))
            }
            kernel.sendNotices(target, adventure, presentation, playerId, notices)
        }
    }

    private fun listRegions(
        source: CommandSourceStack,
        service: RandomTeleportService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val playerId = source.player?.uuid
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.loading_regions"),
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                playerId,
                MovementMessages.randomRegions(service.list()),
            )
        }
    }

    private fun infoRegion(
        source: CommandSourceStack,
        name: String,
        service: RandomTeleportService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val playerId = source.player?.uuid
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.loading_region_info"),
        ) { target ->
            when (val result = service.info(name)) {
                is RegionLookupResult.Found ->
                    kernel.sendDetail(
                        target,
                        adventure,
                        presentation,
                        playerId,
                        MovementMessages.randomRegionInfo(result),
                    )

                RegionLookupResult.InvalidName,
                RegionLookupResult.NotFound ->
                    kernel.sendNotices(
                        target,
                        adventure,
                        presentation,
                        playerId,
                        listOfNotNull(MovementMessages.randomRegionLookupError(result)),
                    )
            }
        }
    }

    private fun createRegion(
        context: CommandContext<CommandSourceStack>,
        name: String,
        minimumRadius: Double,
        maximumRadius: Double,
        service: RandomTeleportService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                ),
            )
        val playerId = player.uuid
        val center = player.toStoredPosition()

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.saving_region"),
        ) { target ->
            val notice = MovementMessages.randomRegionSet(
                name,
                service.create(name, center, minimumRadius, maximumRadius),
            )
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

    private fun centerRegion(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: RandomTeleportService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                ),
            )
        val playerId = player.uuid
        val center = player.toStoredPosition()

        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.saving_region"),
        ) { target ->
            val notice = MovementMessages.randomRegionSet(
                name,
                service.setCenter(name, center),
            )
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

    private fun rangeRegion(
        source: CommandSourceStack,
        name: String,
        minimumRadius: Double,
        maximumRadius: Double,
        service: RandomTeleportService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val playerId = source.player?.uuid
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.saving_region"),
        ) { target ->
            val notice = MovementMessages.randomRegionSet(
                name,
                service.setRange(name, minimumRadius, maximumRadius),
            )
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

    private fun removeRegion(
        source: CommandSourceStack,
        name: String,
        service: RandomTeleportService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val playerId = source.player?.uuid
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.deleting_region"),
        ) { target ->
            val notice = MovementMessages.randomRegionRemoved(
                name,
                service.removeRegion(name),
            )
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice)
            )
        }
    }

}
