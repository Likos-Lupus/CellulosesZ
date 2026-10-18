package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.location.LastLocationService
import top.likoslupus.cellulosesz.movement.teleport.*
import java.util.*

/**
 * The `/tpo*` override family: `/tpo`, `/tpohere`, `/tpoffline` and `/tpall`. These are the admin
 * escape hatch and are deliberately not gated by `/tptoggle`; every move still funnels through the
 * single [TeleportCoordinator] path so the per-world access gate, safety and preflight stay intact.
 */
internal object OverrideTeleportCommands {

    fun commands(
        teleports: TeleportCoordinator,
        lastLocations: LastLocationService,
        known: KnownPlayerResolver,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tpo",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_O,
            documentation = "movement/tpo",
        ) {
            argument("player", word()) { player ->
                suggests { suggestAll(onlineNames(source)) }
                executesPlayer {
                    executorToPlayer(
                        context,
                        get(player),
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
                argument("other", word()) { other ->
                    suggests { suggestAll(onlineNames(source)) }
                    executes {
                        movePlayerToPlayer(
                            context,
                            get(other),
                            get(player),
                            teleports,
                            teleportSettings,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        },

        command(
            name = "tpohere",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_O_HERE,
            documentation = "movement/tpohere",
        ) {
            argument("player", word()) { player ->
                suggests { suggestAll(onlineNames(source)) }
                executesPlayer {
                    playerHere(
                        context,
                        get(player),
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
            name = "tpoffline",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_OFFLINE,
            documentation = "movement/tpoffline",
        ) {
            argument("player", word()) { player ->
                executesPlayer {
                    executorToOffline(
                        context,
                        get(player),
                        teleports,
                        lastLocations,
                        known,
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
            name = "tpall",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_ALL,
            documentation = "movement/tpall",
        ) {
            executesPlayer {
                everyoneTo(
                    context,
                    null,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
            argument("player", word()) { player ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    everyoneTo(
                        context,
                        get(player),
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
    )

    private fun executorToPlayer(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val executor = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    executor.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    targetName,
                )
            )
        val subjectId = executor.uuid
        val targetId = target.uuid
        val resolvedName = target.name.string
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport"),
        ) { _ ->
            val outcome = execute(
                subjectId,
                TeleportDestination.Player(targetId),
                teleports,
                teleportSettings,
            )
            val notices = when (outcome) {
                is TeleportOutcome.Success ->
                    listOf(MovementMessages.teleportedToPlayer(resolvedName))

                else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
            }
            kernel.sendNotices(
                CommandFeedbackTarget.Player(subjectId),
                adventure,
                presentation,
                subjectId,
                notices,
            )
        }
    }

    private fun movePlayerToPlayer(
        context: CommandContext<CommandSourceStack>,
        subjectName: String,
        destinationName: String,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val actorId = source.player?.uuid
        val subject = PlayerResolver.onlineByName(source.server, subjectName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    actorId,
                    MessageKey("movement.teleport.player_not_online"),
                    subjectName,
                )
            )
        val destination = PlayerResolver.onlineByName(source.server, destinationName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    actorId,
                    MessageKey("movement.teleport.player_not_online"),
                    destinationName,
                )
            )
        val subjectId = subject.uuid
        val destinationId = destination.uuid
        val resolvedSubject = subject.name.string
        val resolvedDestination = destination.name.string
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport"),
        ) { feedback ->
            val outcome = execute(
                subjectId,
                TeleportDestination.Player(destinationId),
                teleports,
                teleportSettings,
            )
            val notices = when (outcome) {
                is TeleportOutcome.Success ->
                    listOf(
                        MovementMessages.teleportedOther(
                            resolvedSubject,
                            resolvedDestination,
                        )
                    )

                else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                actorId,
                notices,
            )
        }
    }

    private fun playerHere(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val executor = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    executor.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    targetName,
                )
            )
        val executorId = executor.uuid
        val targetId = target.uuid
        val resolvedTarget = target.name.string
        val resolvedExecutor = executor.name.string
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport"),
        ) { _ ->
            val outcome = execute(
                targetId,
                TeleportDestination.Player(executorId),
                teleports,
                teleportSettings,
            )
            when (outcome) {
                is TeleportOutcome.Success -> {
                    kernel.sendNotices(
                        CommandFeedbackTarget.Player(executorId),
                        adventure,
                        presentation,
                        executorId,
                        listOf(MovementMessages.teleportedHere(resolvedTarget)),
                    )
                    kernel.sendNotices(
                        CommandFeedbackTarget.Player(targetId),
                        adventure,
                        presentation,
                        targetId,
                        listOf(MovementMessages.teleportedToYou(resolvedExecutor)),
                    )
                }

                else -> kernel.sendNotices(
                    CommandFeedbackTarget.Player(executorId),
                    adventure,
                    presentation,
                    executorId,
                    listOfNotNull(MovementMessages.teleportOutcome(outcome)),
                )
            }
        }
    }

    private fun executorToOffline(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        teleports: TeleportCoordinator,
        lastLocations: LastLocationService,
        known: KnownPlayerResolver,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val executor = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        val executorId = executor.uuid
        val online = PlayerResolver.onlineByName(source.server, targetName)
        val knownIdentity = when {
            online != null -> null
            else -> known.knownByName(targetName)
        }
        val targetId = online?.uuid ?: knownIdentity?.id
        val resolvedName = online?.name?.string ?: knownIdentity?.name ?: targetName
        if (targetId == null) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    executorId,
                    MessageKey("movement.teleport.player_not_online"),
                    targetName,
                ),
            )
        }
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport"),
        ) { _ ->
            val location = lastLocations.lastLocation(targetId)
            val notices = if (location == null) {
                listOf(MovementMessages.noLastLocation(resolvedName))
            } else {
                val outcome = execute(
                    executorId,
                    TeleportDestination.Fixed(location.position),
                    teleports,
                    teleportSettings,
                )
                when (outcome) {
                    is TeleportOutcome.Success ->
                        listOf(MovementMessages.teleportedToOffline(resolvedName))

                    else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
                }
            }
            kernel.sendNotices(
                CommandFeedbackTarget.Player(executorId),
                adventure,
                presentation,
                executorId,
                notices,
            )
        }
    }

    private fun everyoneTo(
        context: CommandContext<CommandSourceStack>,
        destinationName: String?,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val actorId = source.player?.uuid
        val destinationId = when (destinationName) {
            null -> actorId
            else -> {
                val resolved = PlayerResolver.onlineByName(source.server, destinationName)
                    ?: return source.replyError(
                        adventure,
                        messages.renderFor(
                            actorId,
                            MessageKey("movement.teleport.player_not_online"),
                            destinationName,
                        )
                    )
                resolved.uuid
            }
        }
        if (destinationId == null) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        }
        val resolvedDestination = when (destinationName) {
            null -> source.player?.name?.string
            else -> PlayerResolver.onlineById(source.server, destinationId)?.name?.string
                ?: destinationName
        }
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.teleport"),
        ) { feedback ->
            val subjects = kernel.onServerThread {
                PlayerResolver.online(kernel.requireServer())
                        .map { it.uuid }
                        .filter { it != destinationId }
            }
            var moved = 0
            subjects.forEach { subjectId ->
                val outcome = execute(
                    subjectId,
                    TeleportDestination.Player(destinationId),
                    teleports,
                    teleportSettings,
                )
                if (outcome is TeleportOutcome.Success) {
                    moved++
                }
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                actorId,
                listOf(MovementMessages.teleportedAll(moved, resolvedDestination)),
            )
        }
    }

    private suspend fun execute(
        subjectId: UUID,
        destination: TeleportDestination,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
    ): TeleportOutcome =
        teleports.execute(
            TeleportIntent(
                subjectId = subjectId,
                destination = destination,
                cause = TeleportCause.ADMIN,
                policy = teleportPolicyFor(
                    TeleportCause.ADMIN,
                    teleportSettings()
                ),
            )
        )

    private fun onlineNames(source: CommandSourceStack): List<String> =
        source.server.playerList.players.map { it.gameProfile.name }

}
