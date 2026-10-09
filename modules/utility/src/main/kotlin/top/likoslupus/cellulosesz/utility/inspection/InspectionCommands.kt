package top.likoslupus.cellulosesz.utility.inspection

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.reply
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.utility.format.UtilityMessages

internal object InspectionCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: InspectionService,
        known: KnownPlayerResolver,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal("enderchest")
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(UtilityMessages.requiresPlayer())
                        source.reply(UtilityMessages.inspection(service.enderChest(playerId)))
                    }
        )
        dispatcher.register(
            Commands.literal("disposal")
                    .executes { context ->
                        val source = context.source
                        val playerId = source.player?.uuid
                            ?: return@executes source.replyError(UtilityMessages.requiresPlayer())
                        source.reply(UtilityMessages.inspection(service.disposal(playerId)))
                    }
        )
        dispatcher.register(
            Commands.literal("invsee")
                    .requires { it.requiresPermission(permissions, CommandPermissions.INVSEE) }
                    .then(
                        Commands.argument("player", StringArgumentType.word())
                                .suggests { context, builder ->
                                    context.source.server.playerList.players
                                            .forEach { builder.suggest(it.gameProfile.name) }
                                    builder.buildFuture()
                                }
                                .executes { context ->
                                    val source = context.source
                                    val viewerId = source.player?.uuid
                                        ?: return@executes source.replyError(UtilityMessages.requiresPlayer())
                                    val raw = StringArgumentType.getString(context, "player")
                                    val targetId = known.onlineByName(raw)?.id
                                        ?: return@executes source.replyError(
                                            UtilityMessages.prefixed("player not found or offline")
                                        )
                                    source.reply(
                                        UtilityMessages.inspection(
                                            service.inspect(
                                                viewerId,
                                                targetId
                                            )
                                        )
                                    )
                                }
                    )
        )
    }

}
