package top.likoslupus.cellulosesz.movement.teleport.command

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
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
import top.likoslupus.cellulosesz.movement.teleport.TeleportCause
import top.likoslupus.cellulosesz.movement.teleport.TeleportCoordinator

/** `/tp`; every move funnels through [TeleportCoordinator]. */
internal object TpCommand {

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
            name = "tp",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP,
            documentation = "movement/tp",
        ) {
            argument("first", word()) { first ->
                executes {
                    direct(
                        context,
                        get(first),
                        null,
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
                argument("second", word()) { second ->
                    executes {
                        direct(
                            context,
                            get(first),
                            get(second),
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
            }
        },
    )

    private fun direct(
        context: CommandContext<CommandSourceStack>,
        firstName: String,
        secondName: String?,
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
        if (secondName == null) {
            val subject = source.player
                ?: return source.replyError(
                    adventure,
                    messages.renderFor(
                        null,
                        MessageKey("core.error.player_required")
                    )
                )
            val target = PlayerResolver.onlineByName(source.server, firstName)
                ?: return source.replyError(
                    adventure,
                    messages.renderFor(
                        subject.uuid,
                        MessageKey("movement.teleport.player_not_online"),
                        firstName
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

            return when (subject.uuid) {
                target.uuid -> source.replyError(
                    adventure,
                    messages.renderFor(
                        subject.uuid,
                        MessageKey("movement.teleport.already_there")
                    ),
                )

                else -> launchToPlayer(
                    source,
                    subject.uuid,
                    target.uuid,
                    TeleportCause.DIRECT,
                    teleports,
                    teleportSettings,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        }

        val subject = PlayerResolver.onlineByName(source.server, firstName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    source.player?.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    firstName
                ),
            )
        val target = PlayerResolver.onlineByName(source.server, secondName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    source.player?.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    secondName
                ),
            )
        disabledTeleportTarget(
            source,
            subject.uuid,
            subject.name.string,
            permissions,
            preferences,
            adventure,
            presentation,
        )?.let { return it }
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
            subject.uuid,
            target.uuid,
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
