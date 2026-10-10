package top.likoslupus.cellulosesz.utility.kit

import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.time.DurationParser
import top.likoslupus.cellulosesz.utility.command.utilityLaunch
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.time.Duration

internal object KitCommands {

    fun commands(
        service: KitService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
    ): List<CommandDefinition> = listOf(
        command(
            name = "kit",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.KIT,
            documentation = "utility/kit",
        ) {
            argument("name", word()) { _ ->
                suggests { service.configuredNames().forEach { suggest(it) } }
                executes { claim(context, service, kernel) }
            }
        },

        command(
            name = "kits",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.KITS,
            documentation = "utility/kits",
        ) {
            executes { list(context, service, kernel) }
        },

        command(
            name = "showkit",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.SHOW_KIT,
            documentation = "utility/showkit",
        ) {
            argument("name", word()) { _ ->
                suggests { service.configuredNames().forEach { suggest(it) } }
                executes { show(context, service, kernel) }
            }
        },

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
                        kernel
                    )
                }
                literal("once") {
                    executes {
                        create(
                            context,
                            KitReusePolicy.Once,
                            service,
                            kernel
                        )
                    }
                }
                literal("cooldown") {
                    argument("duration", word()) { _ ->
                        executes {
                            createWithDuration(
                                context,
                                service,
                                kernel
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
                        kernel
                    )
                }
                literal("once") {
                    executes {
                        update(
                            context,
                            KitReusePolicy.Once,
                            service,
                            kernel
                        )
                    }
                }
                literal("cooldown") {
                    argument("duration", word()) { _ ->
                        executes {
                            updateWithDuration(
                                context,
                                service,
                                kernel
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
                suggests {
                    service.configuredNames().forEach {
                        suggest(it)
                    }
                }
                executes {
                    delete(
                        context,
                        service,
                        kernel
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
                        kernel
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
                            kernel
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
    ): Int {
        val duration = parseDuration(context)
            ?: return context.source.replyError(UtilityMessages.prefixed("invalid cooldown duration"))
        return create(context, KitReusePolicy.Cooldown(duration), service, kernel)
    }

    private fun updateWithDuration(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val duration = parseDuration(context)
            ?: return context.source.replyError(UtilityMessages.prefixed("invalid cooldown duration"))
        return update(context, KitReusePolicy.Cooldown(duration), service, kernel)
    }

    private fun parseDuration(context: CommandContext<CommandSourceStack>): Duration? =
        DurationParser.parse(getString(context, "duration"))?.duration

    private fun claim(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(UtilityMessages.requiresPlayer())
        val name = getString(context, "name")

        return utilityLaunch(source, kernel, "claiming kit...") {
            kernel.message(
                target = it,
                message = UtilityMessages.claim(
                    name,
                    service.claim(playerId, name)
                ),
            )
        }
    }

    private fun list(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        return utilityLaunch(source, kernel, "loading kits...") {
            kernel.message(
                target = it,
                message = UtilityMessages.list(service.list(playerId))
            )
        }
    }

    private fun show(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val name = getString(context, "name")
        return utilityLaunch(source, kernel, "loading kit...") {
            kernel.message(
                it,
                UtilityMessages.show(service.show(name))
            )
        }
    }

    private fun create(
        context: CommandContext<CommandSourceStack>,
        reuse: KitReusePolicy,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(UtilityMessages.requiresPlayer())
        val name = getString(context, "name")
        return utilityLaunch(source, kernel, "creating kit...") {
            kernel.message(
                target = it,
                message = UtilityMessages.create(
                    service.create(
                        playerId,
                        name,
                        reuse
                    )
                ),
            )
        }
    }

    private fun update(
        context: CommandContext<CommandSourceStack>,
        reuse: KitReusePolicy?,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(UtilityMessages.requiresPlayer())
        val name = getString(context, "name")
        return utilityLaunch(source, kernel, "updating kit...") {
            kernel.message(
                target = it,
                message = UtilityMessages.update(
                    service.update(
                        playerId,
                        name,
                        reuse
                    )
                ),
            )
        }
    }

    private fun delete(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val name = getString(context, "name")
        return utilityLaunch(source, kernel, "deleting kit...") {
            kernel.message(
                target = it,
                message = UtilityMessages.delete(
                    service.delete(name)
                )
            )
        }
    }

    private fun resetSelf(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(UtilityMessages.requiresPlayer())
        return reset(
            source = source,
            target = KnownPlayerIdentity(player.uuid, player.gameProfile.name),
            name = getString(context, "name"),
            service = service,
            kernel = kernel,
        )
    }

    private fun resetOther(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val raw = getString(context, "player")
        val target = known.onlineByName(raw) ?: known.knownByName(raw)
        return when (target) {
            null -> source.replyError(
                UtilityMessages.prefixed("player '$raw' is not known to this server")
            )

            else -> reset(
                source,
                target = target,
                name = getString(context, "name"),
                service = service,
                kernel = kernel,
            )
        }
    }

    private fun reset(
        source: CommandSourceStack,
        target: KnownPlayerIdentity,
        name: String,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int =
        utilityLaunch(source, kernel, "resetting kit...") {
            kernel.message(
                target = it,
                message = UtilityMessages.reset(
                    service.reset(
                        name,
                        target
                    )
                ),
            )
        }

}
