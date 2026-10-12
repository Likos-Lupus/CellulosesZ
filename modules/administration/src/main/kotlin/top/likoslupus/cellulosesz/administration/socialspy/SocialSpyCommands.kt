package top.likoslupus.cellulosesz.administration.socialspy

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices

/** `/socialspy [on|off]` — session-only self toggle. */
internal object SocialSpyCommands {

    fun commands(
        service: SocialSpyService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "socialspy",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.SOCIAL_SPY,
            documentation = "administration/socialspy",
        ) {
            executesPlayer {
                apply(
                    context,
                    null,
                    service,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
            literal("on") {
                executesPlayer {
                    apply(
                        context,
                        true,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
            literal("off") {
                executesPlayer {
                    apply(
                        context,
                        false,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },
    )

    private fun apply(
        context: CommandContext<CommandSourceStack>,
        enable: Boolean?,
        service: SocialSpyService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(null, MessageKey("core.error.player_required")),
            )
        val desired = enable
            ?: !service.isSpying(player.uuid)
        val actor = source.moderationActor()
        val identity = PlayerIdentity(player.uuid, player.gameProfile.name)

        return moderationLaunch(
            source,
            kernel,
            messages,
            adventure,
            presentation,
            LocalizedText(MessageKey("administration.pending.socialspy")),
        ) { feedback ->
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                identity.id,
                listOf(
                    AdministrationMessages.socialSpy(
                        service.set(
                            actor,
                            identity,
                            desired
                        )
                    )
                ),
            )
        }
    }

}
