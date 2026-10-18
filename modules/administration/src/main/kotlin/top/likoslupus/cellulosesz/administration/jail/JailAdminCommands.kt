package top.likoslupus.cellulosesz.administration.jail

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.format.*
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
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
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyPage
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import java.time.Instant

/**
 * The jail-location administration commands: `/setjail`, `/deljail`, `/jails` and
 * `/jailedplayers`. `/setjail` is declared by [setJail] on its own so [JailCommands] can keep the
 * original registration order; [commands] declares the remaining three.
 */
internal object JailAdminCommands {

    fun setJail(
        jail: JailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition = command(
        name = "setjail",
        category = CommandCategory.ADMINISTRATION,
        permission = CommandPermissions.SET_JAIL,
        documentation = "administration/setjail",
    ) {
        argument("name", word()) { name ->
            executesPlayer {
                applySetJail(
                    context,
                    get(name),
                    jail,
                    kernel,
                    messages,
                    adventure,
                    presentation,
                )
            }
        }
    }

    fun commands(
        jail: JailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "deljail",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.DEL_JAIL,
            documentation = "administration/deljail",
        ) {
            argument("name", word()) { name ->
                executes {
                    deleteJail(
                        context,
                        get(name),
                        jail,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },

        command(
            name = "jails",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.JAILS,
            documentation = "administration/jails",
        ) {
            executes {
                val source = context.source
                source.replyPage(
                    adventure,
                    presentation,
                    source.player?.uuid,
                    AdministrationMessages.jails(jail.locations()),
                )
            }
        },

        command(
            name = "jailedplayers",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.JAILED_PLAYERS,
            documentation = "administration/jailedplayers",
        ) {
            executes {
                val source = context.source
                source.replyPage(
                    adventure,
                    presentation,
                    source.player?.uuid,
                    AdministrationMessages.jailedPlayers(
                        jail.sentences(),
                        Instant.now(),
                    ),
                )
            }
        },
    )

    private fun applySetJail(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        jail: JailService,
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
        val destination = JailDestination(
            dimension = player.level().dimension().identifier().toString(),
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yRot,
            pitch = player.xRot,
        )
        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.setjail")),
        ) { feedback ->
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(
                    AdministrationMessages.setJail(
                        jail.setLocation(
                            rawName,
                            destination,
                            player.gameProfile.name,
                        )
                    )
                ),
            )
        }
    }

    private fun deleteJail(
        context: CommandContext<CommandSourceStack>,
        rawName: String,
        jail: JailService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.deljail")),
        ) { feedback ->
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(
                    AdministrationMessages.deleteJail(
                        rawName,
                        jail.deleteLocation(rawName)
                    )
                ),
            )
        }
    }

}
