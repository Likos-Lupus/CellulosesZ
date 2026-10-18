package top.likoslupus.cellulosesz.utility.kit

import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.foundation.time.DurationParser
import top.likoslupus.cellulosesz.utility.command.utilityLaunch
import top.likoslupus.cellulosesz.utility.format.*
import java.time.Duration

internal object KitAdminCommands {

    fun commands(
        service: KitService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "createkit",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.CREATE_KIT,
            documentation = "utility/createkit",
        ) {
            argument("name", word()) { _ ->
                executes {
                    create(
                        context,
                        KitReusePolicy.Always,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
                literal("once") {
                    executes {
                        create(
                            context,
                            KitReusePolicy.Once,
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
                literal("cooldown") {
                    argument("duration", word()) { _ ->
                        executes {
                            createWithDuration(
                                context,
                                service,
                                kernel,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                    }
                }
            }
        },

        command(
            name = "updatekit",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.UPDATE_KIT,
            documentation = "utility/updatekit",
        ) {
            argument("name", word()) { _ ->
                suggests { service.configuredNames().forEach { suggest(it) } }
                executes {
                    update(
                        context,
                        null,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
                literal("once") {
                    executes {
                        update(
                            context,
                            KitReusePolicy.Once,
                            service,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
                literal("cooldown") {
                    argument("duration", word()) { _ ->
                        executes {
                            updateWithDuration(
                                context,
                                service,
                                kernel,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                    }
                }
            }
        },

        command(
            name = "delkit",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.DEL_KIT,
            documentation = "utility/delkit",
        ) {
            argument("name", word()) { _ ->
                suggests { service.configuredNames().forEach { suggest(it) } }
                executes {
                    delete(
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
            name = "kitreset",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.KIT_RESET,
            documentation = "utility/kitreset",
        ) {
            argument("name", word()) { _ ->
                suggests { service.configuredNames().forEach { suggest(it) } }
                executes {
                    resetSelf(
                        context,
                        service,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument("player", word()) { _ ->
                    suggests {
                        source.server.playerList.players.forEach {
                            suggest(it.gameProfile.name)
                        }
                    }
                    executes {
                        resetOther(
                            context,
                            service,
                            known,
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

    private fun createWithDuration(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val duration = parseDuration(context)
            ?: return context.source.replyError(
                adventure,
                messages.renderFor(
                    context.source.player?.uuid,
                    MessageKey("utility.kit.invalid_cooldown")
                ),
            )
        return create(
            context,
            KitReusePolicy.Cooldown(duration),
            service,
            kernel,
            messages,
            adventure,
            presentation
        )
    }

    private fun updateWithDuration(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val duration = parseDuration(context)
            ?: return context.source.replyError(
                adventure,
                messages.renderFor(
                    context.source.player?.uuid,
                    MessageKey("utility.kit.invalid_cooldown")
                ),
            )
        return update(
            context,
            KitReusePolicy.Cooldown(duration),
            service,
            kernel,
            messages,
            adventure,
            presentation
        )
    }

    private fun parseDuration(context: CommandContext<CommandSourceStack>): Duration? =
        DurationParser.parse(getString(context, "duration"))?.duration

    private fun create(
        context: CommandContext<CommandSourceStack>,
        reuse: KitReusePolicy,
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
            MessageKey("utility.pending.creating")
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(
                    UtilityMessages.create(
                        service.create(
                            playerId,
                            name,
                            reuse
                        )
                    )
                )
            )
        }
    }

    private fun update(
        context: CommandContext<CommandSourceStack>,
        reuse: KitReusePolicy?,
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
            MessageKey("utility.pending.updating")
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(
                    UtilityMessages.update(
                        service.update(
                            playerId,
                            name,
                            reuse
                        )
                    )
                )
            )
        }
    }

    private fun delete(
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
            MessageKey("utility.pending.deleting")
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                source.player?.uuid,
                listOf(UtilityMessages.delete(service.delete(name)))
            )
        }
    }

    private fun resetSelf(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val player = context.source.player
            ?: return context.source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        return reset(
            context = context,
            target = KnownPlayerIdentity(player.uuid, player.gameProfile.name),
            name = getString(context, "name"),
            service = service,
            kernel = kernel,
            messages = messages,
            adventure = adventure,
            presentation = presentation,
        )
    }

    private fun resetOther(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val raw = getString(context, "player")
        val target = known.onlineByName(raw) ?: known.knownByName(raw)
        return when (target) {
            null -> source.replyError(
                adventure,
                messages.renderFor(
                    source.player?.uuid,
                    MessageKey("utility.kit.player_unknown"),
                    raw
                ),
            )

            else -> reset(
                context = context,
                target = target,
                name = getString(context, "name"),
                service = service,
                kernel = kernel,
                messages = messages,
                adventure = adventure,
                presentation = presentation,
            )
        }
    }

    private fun reset(
        context: CommandContext<CommandSourceStack>,
        target: KnownPlayerIdentity,
        name: String,
        service: KitService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val playerId = context.source.player?.uuid
        return utilityLaunch(
            context.source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.resetting")
        ) { feedback ->
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(
                    UtilityMessages.reset(
                        service.reset(name, target)
                    )
                )
            )
        }
    }

}
