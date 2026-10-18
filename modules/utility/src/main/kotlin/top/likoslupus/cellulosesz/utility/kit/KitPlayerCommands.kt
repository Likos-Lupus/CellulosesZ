package top.likoslupus.cellulosesz.utility.kit

import com.mojang.brigadier.arguments.StringArgumentType.getString
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
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.core.text.presentation.sendPage
import top.likoslupus.cellulosesz.utility.command.utilityLaunch
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import top.likoslupus.cellulosesz.utility.format.claim
import top.likoslupus.cellulosesz.utility.format.list
import top.likoslupus.cellulosesz.utility.format.show

internal object KitPlayerCommands {

    fun commands(
        service: KitService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "kit",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.KIT,
            documentation = "utility/kit",
        ) {
            argument("name", word()) { _ ->
                suggests { service.configuredNames().forEach { suggest(it) } }
                executes {
                    claim(
                        context,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "kits",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.KITS,
            documentation = "utility/kits",
        ) {
            executes {
                list(
                    context,
                    service,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
        },

        command(
            name = "showkit",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.SHOW_KIT,
            documentation = "utility/showkit",
        ) {
            argument("name", word()) { _ ->
                suggests { service.configuredNames().forEach { suggest(it) } }
                executes {
                    show(
                        context,
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

    private fun claim(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
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
        val name = getString(context, "name")

        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.claiming")
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                UtilityMessages.claim(
                    name,
                    service.claim(playerId, name)
                )
            )
        }
    }

    private fun list(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.loading_kits")
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                playerId,
                UtilityMessages.list(service.list(playerId))
            )
        }
    }

    private fun show(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val name = getString(context, "name")
        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.loading_kit")
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                source.player?.uuid,
                UtilityMessages.show(service.show(name))
            )
        }
    }

}
