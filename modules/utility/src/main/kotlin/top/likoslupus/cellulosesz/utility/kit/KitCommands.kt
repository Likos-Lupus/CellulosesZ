package top.likoslupus.cellulosesz.utility.kit

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import top.likoslupus.cellulosesz.core.command.message
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.time.DurationParser
import top.likoslupus.cellulosesz.utility.command.utilityLaunch
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.time.Duration

internal object KitCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: KitService,
        known: KnownPlayerResolver,
        kernel: RuntimeKernel,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            literal("kit")
                    .then(
                        argument("name", word())
                                .suggests { _, builder ->
                                    service.configuredNames().forEach(builder::suggest)
                                    builder.buildFuture()
                                }
                                .executes { context -> claim(context, service, kernel) }
                    )
        )
        dispatcher.register(
            literal("kits")
                    .executes { context -> list(context, service, kernel) }
        )
        dispatcher.register(
            literal("showkit")
                    .then(
                        argument("name", word())
                                .suggests { _, builder ->
                                    service.configuredNames().forEach(builder::suggest)
                                    builder.buildFuture()
                                }
                                .executes { context -> show(context, service, kernel) }
                    )
        )
        dispatcher.register(
            literal("createkit")
                    .requires { it.requiresPermission(permissions, CommandPermissions.CREATE_KIT) }
                    .then(
                        argument("name", word())
                                .executes { context ->
                                    create(context, KitReusePolicy.Always, service, kernel)
                                }
                                .then(
                                    literal("once")
                                            .executes { context ->
                                                create(
                                                    context,
                                                    KitReusePolicy.Once,
                                                    service,
                                                    kernel
                                                )
                                            }
                                )
                                .then(
                                    literal("cooldown")
                                            .then(
                                                argument("duration", word())
                                                        .executes { context ->
                                                            val duration = parseDuration(context)
                                                                ?: return@executes context.source.replyError(
                                                                    UtilityMessages.prefixed("invalid cooldown duration")
                                                                )
                                                            create(
                                                                context,
                                                                KitReusePolicy.Cooldown(duration),
                                                                service,
                                                                kernel,
                                                            )
                                                        }
                                            )
                                )
                    )
        )
        dispatcher.register(
            literal("updatekit")
                    .requires { it.requiresPermission(permissions, CommandPermissions.UPDATE_KIT) }
                    .then(
                        argument("name", word())
                                .suggests { _, builder ->
                                    service.configuredNames().forEach(builder::suggest)
                                    builder.buildFuture()
                                }
                                .executes { context ->
                                    update(context, null, service, kernel)
                                }
                                .then(
                                    literal("once")
                                            .executes { context ->
                                                update(
                                                    context,
                                                    KitReusePolicy.Once,
                                                    service,
                                                    kernel
                                                )
                                            }
                                )
                                .then(
                                    literal("cooldown")
                                            .then(
                                                argument("duration", word())
                                                        .executes { context ->
                                                            val duration = parseDuration(context)
                                                                ?: return@executes context.source.replyError(
                                                                    UtilityMessages.prefixed("invalid cooldown duration")
                                                                )
                                                            update(
                                                                context,
                                                                KitReusePolicy.Cooldown(duration),
                                                                service,
                                                                kernel,
                                                            )
                                                        }
                                            )
                                )
                    )
        )
        dispatcher.register(
            literal("delkit")
                    .requires { it.requiresPermission(permissions, CommandPermissions.DEL_KIT) }
                    .then(
                        argument("name", word())
                                .suggests { _, builder ->
                                    service.configuredNames().forEach(builder::suggest)
                                    builder.buildFuture()
                                }
                                .executes { context -> delete(context, service, kernel) }
                    )
        )
        dispatcher.register(
            literal("kitreset")
                    .requires { it.requiresPermission(permissions, CommandPermissions.KIT_RESET) }
                    .then(
                        argument("name", word())
                                .suggests { _, builder ->
                                    service.configuredNames().forEach(builder::suggest)
                                    builder.buildFuture()
                                }
                                .executes { context -> resetSelf(context, service, kernel) }
                                .then(
                                    argument("player", word())
                                            .suggests { context, builder ->
                                                context.source.server.playerList.players
                                                        .forEach { builder.suggest(it.gameProfile.name) }
                                                builder.buildFuture()
                                            }
                                            .executes { context ->
                                                resetOther(context, service, known, kernel)
                                            }
                                )
                    )
        )
    }

    private fun parseDuration(context: CommandContext<CommandSourceStack>): Duration? =
        DurationParser
                .parse(getString(context, "duration"))
                ?.duration

    private fun claim(
        context: CommandContext<CommandSourceStack>,
        service: KitService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(UtilityMessages.requiresPlayer())
        val name = getString(context, "name")

        return utilityLaunch(
            source,
            kernel,
            "claiming kit..."
        ) {
            kernel.message(
                target = it,
                message = UtilityMessages.claim(
                    requestedName = name,
                    result = service.claim(playerId, name)
                )
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
        return utilityLaunch(
            source,
            kernel,
            "loading kits..."
        ) {
            kernel.message(
                target = it,
                message = UtilityMessages.list(
                    service.list(playerId)
                )
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
        return utilityLaunch(
            source,
            kernel,
            "loading kit..."
        ) {
            kernel.message(
                it,
                UtilityMessages.show(
                    service.show(
                        name
                    )
                )
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
        return utilityLaunch(
            source,
            kernel,
            "creating kit..."
        ) {
            kernel.message(
                target = it,
                message = UtilityMessages.create(
                    service.create(
                        playerId = playerId,
                        rawName = name,
                        reuse = reuse
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
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(UtilityMessages.requiresPlayer())
        val name = getString(context, "name")
        return utilityLaunch(
            source,
            kernel,
            "updating kit..."
        ) {
            kernel.message(
                target = it,
                message = UtilityMessages.update(
                    service.update(
                        playerId = playerId,
                        rawName = name,
                        reuse = reuse
                    )
                )
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
        return utilityLaunch(
            source,
            kernel,
            "deleting kit..."
        ) {
            kernel.message(
                target = it,
                message = UtilityMessages.delete(
                    service.delete(
                        name
                    )
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
            null -> source.replyError(UtilityMessages.prefixed("player '$raw' is not known to this server"))
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
        utilityLaunch(
            source,
            kernel,
            "resetting kit..."
        ) {
            kernel.message(
                target = it,
                message = UtilityMessages.reset(
                    service.reset(
                        rawName = name,
                        target = target
                    )
                )
            )
        }

}
