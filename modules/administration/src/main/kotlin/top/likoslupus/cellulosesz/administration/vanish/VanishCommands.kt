package top.likoslupus.cellulosesz.administration.vanish

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.command.ModerationFeedback
import top.likoslupus.cellulosesz.administration.moderation.command.moderationLaunch
import top.likoslupus.cellulosesz.administration.moderation.moderationActor
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

/** `/vanish [on|off]` — self-only, session-only. */
internal object VanishCommands {

    fun commands(
        service: VanishService,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "vanish",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.VANISH,
            documentation = "administration/vanish",
        ) {
            executesPlayer {
                apply(
                    context,
                    null,
                    service,
                    kernel
                )
            }
            literal("on") {
                executesPlayer {
                    apply(
                        context,
                        true,
                        service,
                        kernel
                    )
                }
            }
            literal("off") {
                executesPlayer {
                    apply(
                        context,
                        false,
                        service,
                        kernel
                    )
                }
            }
        },
    )

    private fun apply(
        context: CommandContext<CommandSourceStack>,
        enable: Boolean?,
        service: VanishService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(Messages.prefixed("this command requires a player"))
        val desired = enable
            ?: !service.isVanished(player.uuid)
        val actor = source.moderationActor()
        val identity = PlayerIdentity(player.uuid, player.gameProfile.name)

        return moderationLaunch(
            source,
            kernel,
            "updating vanish..."
        ) {
            feedback(
                service.set(
                    actor,
                    identity,
                    desired
                )
            )
        }
    }

    private fun feedback(result: VanishResult): Component {
        val state = if (result.enabled) "enabled" else "disabled"
        val note = ModerationFeedback.auditNote(result.auditRecorded)
        return Messages.prefixed("vanish $state$note")
    }

}
