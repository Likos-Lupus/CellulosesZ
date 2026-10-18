package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.double
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.TeleportCause
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator

/** `/tphere` and `/tppos`; every move funnels through [TeleportCoordinator]. */
internal object TpHerePosCommands {

    fun commands(
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tphere",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_HERE,
            documentation = "movement/tphere",
        ) {
            argument("target", word()) { target ->
                executesPlayer {
                    teleportHere(
                        context,
                        get(target),
                        teleports,
                        teleportSettings,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                        permissions,
                        preferences,
                    )
                }
            }
        },

        command(
            name = "tppos",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_POS,
            documentation = "movement/tppos",
        ) {
            argument("x", double()) { x ->
                argument("y", double()) { y ->
                    argument("z", double()) { z ->
                        executesPlayer {
                            teleportPos(
                                context,
                                null,
                                get(x),
                                get(y),
                                get(z),
                                teleports,
                                teleportSettings,
                                kernel,
                                messages,
                                adventure,
                                presentation,
                            )
                        }
                        argument("dimension", word()) { dimension ->
                            executesPlayer {
                                teleportPos(
                                    context,
                                    get(dimension),
                                    get(x),
                                    get(y),
                                    get(z),
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
                }
            }
        },
    )

    private fun teleportHere(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        preferences: PlayerPreferenceService,
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
                    targetName
                ),
            )
        disabledTeleportTarget(
            source,
            target.uuid,
            target.name.string,
            permissions,
            preferences,
            adventure,
            presentation,
        )?.let { return it }
        return launchToPlayer(
            source,
            target.uuid,
            executor.uuid,
            TeleportCause.ADMIN,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
        )
    }

    private fun teleportPos(
        context: CommandContext<CommandSourceStack>,
        dimension: String?,
        x: Double,
        y: Double,
        z: Double,
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

        val resolvedDimension = dimension
            ?: executor.level().dimension().identifier().toString()
        if (Identifier.tryParse(resolvedDimension) == null) {
            return source.replyError(
                adventure,
                messages.renderFor(
                    executor.uuid,
                    MessageKey("movement.teleport.invalid_dimension"),
                    resolvedDimension
                ),
            )
        }

        val position = StoredPosition(
            dimension = resolvedDimension,
            x = x,
            y = y,
            z = z,
            yaw = executor.yRot,
            pitch = executor.xRot,
        )
        return launchToPosition(
            source,
            executor.uuid,
            position,
            TeleportCause.ADMIN,
            teleports,
            teleportSettings,
            kernel,
            messages,
            adventure,
            presentation,
        )
    }

}
