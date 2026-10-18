package top.likoslupus.cellulosesz.utility.unlimited

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
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
import java.util.*

internal object UnlimitedCommands {

    fun commands(
        service: UnlimitedService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "unlimited",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.UNLIMITED,
            documentation = "utility/unlimited",
        ) {
            literal("list") {
                executesPlayer {
                    list(
                        context.source,
                        playerId,
                        null,
                        known,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.UNLIMITED_OTHERS,
                ) { player ->
                    suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                    executes {
                        list(
                            context.source,
                            null,
                            get(player),
                            known,
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
            literal("clear") {
                executesPlayer {
                    clear(
                        context.source,
                        playerId,
                        null,
                        known,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.UNLIMITED_OTHERS,
                ) { player ->
                    suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                    executes {
                        clear(
                            context.source,
                            null,
                            get(player),
                            known,
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
            literal("item") {
                executesPlayer {
                    toggle(
                        context.source,
                        playerId,
                        null,
                        known,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.UNLIMITED_OTHERS,
                ) { player ->
                    suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                    executes {
                        toggle(
                            context.source,
                            null,
                            get(player),
                            known,
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },
    )

    private fun list(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        known: KnownPlayerResolver,
        service: UnlimitedService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val targetId = resolve(source, actorId, targetName, known, messages, adventure) ?: return 0
        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.unlimited"),
        ) { target ->
            kernel.sendPage(
                target,
                adventure,
                presentation,
                actorId,
                UtilityMessages.unlimitedList(service.list(targetId)),
            )
        }
    }

    private fun clear(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        known: KnownPlayerResolver,
        service: UnlimitedService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val targetId = resolve(source, actorId, targetName, known, messages, adventure) ?: return 0
        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.unlimited"),
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                actorId,
                listOf(UtilityMessages.unlimitedClear(service.clear(targetId))),
            )
        }
    }

    private fun toggle(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        known: KnownPlayerResolver,
        service: UnlimitedService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val targetId = resolve(source, actorId, targetName, known, messages, adventure) ?: return 0
        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.unlimited"),
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                actorId,
                listOf(UtilityMessages.unlimitedToggle(service.toggle(targetId))),
            )
        }
    }

    private fun resolve(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): UUID? {
        val resolved = when (targetName) {
            null -> actorId
            else -> known.onlineByName(targetName)?.id
        }
        if (resolved == null) {
            source.replyError(
                adventure,
                messages.renderFor(
                    actorId,
                    MessageKey("utility.error.player_not_found"),
                ),
            )
        }
        return resolved
    }

}
