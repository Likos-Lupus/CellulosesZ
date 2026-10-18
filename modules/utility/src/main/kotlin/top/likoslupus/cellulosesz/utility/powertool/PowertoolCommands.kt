package top.likoslupus.cellulosesz.utility.powertool

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.BuiltInRegistries
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.core.text.presentation.sendPage
import top.likoslupus.cellulosesz.utility.command.utilityLaunch
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.util.*

internal object PowertoolCommands {

    fun commands(
        service: PowertoolService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "powertool",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.POWERTOOL,
            documentation = "utility/powertool",
        ) {
            executesPlayer {
                bind(
                    context.source,
                    playerId,
                    PowertoolSpec.Clear,
                    service,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("command", greedyString()) { command ->
                executesPlayer {
                    bind(
                        context.source,
                        playerId,
                        PowertoolSpec.parse(get(command)),
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
            name = "powertoollist",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.POWERTOOL_LIST,
            documentation = "utility/powertoollist",
        ) {
            executesPlayer {
                list(
                    context.source,
                    playerId,
                    service,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
        },

        command(
            name = "powertooltoggle",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.POWERTOOL_TOGGLE,
            documentation = "utility/powertooltoggle",
        ) {
            executesPlayer {
                toggle(
                    context.source,
                    playerId,
                    service,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
        },
    )

    private fun bind(
        source: CommandSourceStack,
        playerId: UUID,
        spec: PowertoolSpec,
        service: PowertoolService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        if (!service.enabled()) {
            return source.replyNotice(
                adventure,
                presentation,
                playerId,
                UtilityMessages.powertoolDisabled(),
            )
        }
        val itemId = heldItemId(source)
        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.powertool"),
        ) { target ->
            val result = service.bind(playerId, itemId, spec)
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(
                    when (result) {
                        is PowertoolBindResult.Bound ->
                            UtilityMessages.powertoolBound(result.itemId, result.trigger)

                        is PowertoolBindResult.Cleared ->
                            UtilityMessages.powertoolCleared(result.itemId)

                        PowertoolBindResult.NoItemId -> UtilityMessages.powertoolNoItem()
                        PowertoolBindResult.Recursive -> UtilityMessages.powertoolRejected(true)
                        PowertoolBindResult.InvalidCommand -> UtilityMessages.powertoolRejected(
                            false
                        )

                        PowertoolBindResult.StorageUnavailable -> UtilityMessages.powertoolStorage()
                    }
                ),
            )
        }
    }

    private fun list(
        source: CommandSourceStack,
        playerId: UUID,
        service: PowertoolService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.powertool"),
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                playerId,
                UtilityMessages.powertoolList(service.list(playerId)),
            )
        }

    private fun toggle(
        source: CommandSourceStack,
        playerId: UUID,
        service: PowertoolService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.powertool"),
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(UtilityMessages.powertoolToggle(service.toggle(playerId))),
            )
        }

    private fun heldItemId(source: CommandSourceStack): String? {
        val stack = source.player?.mainHandItem ?: return null
        if (stack.isEmpty) {
            return null
        }
        return BuiltInRegistries.ITEM.getKey(stack.item).toString()
    }

}
