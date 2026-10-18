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
import top.likoslupus.cellulosesz.movement.format.noLastLocation
import top.likoslupus.cellulosesz.movement.format.teleportOutcome
import top.likoslupus.cellulosesz.movement.format.teleportedToOffline
import top.likoslupus.cellulosesz.movement.location.LastLocationService
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator
import top.likoslupus.cellulosesz.movement.teleport.TeleportDestination
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome

/** `/tpoffline`: teleports the executor to an online or known player's last recorded position. */
internal object OfflineTeleportCommand {

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
    )

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
                val outcome = executeAdminTeleport(
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

}
