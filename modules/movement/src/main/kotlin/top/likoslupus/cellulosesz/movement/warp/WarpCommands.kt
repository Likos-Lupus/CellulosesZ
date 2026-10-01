package top.likoslupus.cellulosesz.movement.warp

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.canUseModeratorCommands
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.command.replyError
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.movement.command.launchResult
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.teleport.*
import top.likoslupus.cellulosesz.movement.teleport.command.TeleportFeedback

internal object WarpCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        service: WarpService,
        backend: TeleportBackend,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal("warp")
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    useWarp(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        teleports,
                                        teleportSettings,
                                        kernel,
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("warps")
                    .executes { context -> listWarps(context, service, kernel) }
        )
        dispatcher.register(
            Commands.literal("setwarp")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    setWarp(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        backend,
                                        kernel,
                                    )
                                }
                    )
        )
        dispatcher.register(
            Commands.literal("delwarp")
                    .requires { it.canUseModeratorCommands() }
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                                .executes { context ->
                                    deleteWarp(
                                        context,
                                        StringArgumentType.getString(context, "name"),
                                        service,
                                        kernel,
                                    )
                                }
                    )
        )
    }

    private fun useWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
        teleports: TeleportCoordinator,
        teleportSettings: () -> TeleportSettings,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val message = when (val result = service.get(name)) {
                is WarpLookupResult.Found -> {
                    val intent = TeleportIntent(
                        subjectId = playerId,
                        destination = TeleportDestination.Fixed(result.warp.position),
                        cause = TeleportCause.WARP,
                        policy = teleportPolicyFor(TeleportCause.WARP, teleportSettings()),
                    )

                    when (val outcome = teleports.execute(intent)) {
                        is TeleportOutcome.Success ->
                            Messages.prefixed("teleported to warp '$name'")

                        else -> TeleportFeedback.failure(outcome)
                            ?: return@launch
                    }
                }

                WarpLookupResult.InvalidName ->
                    Messages.prefixed("invalid warp name")

                WarpLookupResult.NotFound ->
                    Messages.prefixed("warp '$name' not found")
            }
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "teleporting...")
    }

    private fun listWarps(
        context: CommandContext<CommandSourceStack>,
        service: WarpService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val warps = service.list().map { it.name.value }
            val message = Messages.prefixed(
                when {
                    warps.isEmpty() -> "no warps defined"
                    else -> "warps: ${warps.joinToString(", ")}"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "loading warps...")
    }

    private fun setWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
        backend: TeleportBackend,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val position = backend.position(playerId)
            val message = Messages.prefixed(
                when (position) {
                    null -> "you are no longer online"
                    else -> when (service.set(name, position)) {
                        SetWarpResult.Success -> "warp '$name' created"
                        SetWarpResult.InvalidName -> "invalid warp name"
                    }
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "saving warp...")
    }

    private fun deleteWarp(
        context: CommandContext<CommandSourceStack>,
        name: String,
        service: WarpService,
        kernel: RuntimeKernel,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
            ?: return source.replyError(Messages.prefixed("this command requires a player"))

        val job = kernel.launch {
            val message = Messages.prefixed(
                when (service.delete(name)) {
                    DeleteWarpResult.Deleted -> "warp '$name' deleted"
                    DeleteWarpResult.InvalidName -> "invalid warp name"
                    DeleteWarpResult.NotFound -> "warp '$name' not found"
                }
            )
            kernel.messagePlayer(playerId, message)
        }

        return launchResult(source, job, "deleting warp...")
    }

}
