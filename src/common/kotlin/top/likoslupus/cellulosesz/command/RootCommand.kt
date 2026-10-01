package top.likoslupus.cellulosesz.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.permissions.Permissions
import top.likoslupus.cellulosesz.config.ConfigReloadResult
import top.likoslupus.cellulosesz.config.ConfigService
import top.likoslupus.cellulosesz.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.text.Messages

internal object RootCommand {

    const val ROOT_LITERAL: String = "cellulosesz"

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        config: ConfigService,
        kernel: RuntimeKernel,
    ) {
        dispatcher.register(
            Commands.literal(ROOT_LITERAL)
                    .requires { source ->
                        source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)
                    }
                    .then(
                        Commands.literal("status")
                                .executes { context ->
                                    val source = context.source
                                    source.sendSuccess(
                                        {
                                            Messages.prefixed(
                                                "state=${kernel.state} configGeneration=${config.generation}"
                                            )
                                        },
                                        false,
                                    )
                                    1
                                }
                    )
                    .then(
                        Commands.literal("reload")
                                .executes { context ->
                                    val source = context.source
                                    val job = kernel.launchIo {
                                        when (val result = config.reload()) {
                                            is ConfigReloadResult.Success -> kernel.onServerThread {
                                                source.sendSuccess(
                                                    { Messages.prefixed("config reloaded (generation ${result.generation})") },
                                                    true,
                                                )
                                            }

                                            is ConfigReloadResult.Failure -> kernel.onServerThread {
                                                source.sendFailure(
                                                    Messages.prefixed(
                                                        "config reload failed: ${
                                                            result.errors.joinToString(
                                                                "; "
                                                            )
                                                        }"
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    if (job == null) {
                                        source.sendFailure(Messages.prefixed("runtime is shutting down; reload rejected"))
                                        0
                                    } else {
                                        source.sendSuccess(
                                            { Messages.prefixed("reloading config...") },
                                            false
                                        )
                                        1
                                    }
                                }
                    )
        )
    }

}
