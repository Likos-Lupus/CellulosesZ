package top.likoslupus.cellulosesz.application.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import top.likoslupus.cellulosesz.foundation.config.ConfigStore

internal object RootCommand {

    const val ROOT_LITERAL: String = "cellulosesz"

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        config: ConfigStore<CellulosesConfig>,
        kernel: RuntimeKernel,
        health: ApplicationHealth,
        permissions: PermissionService,
    ) {
        dispatcher.register(
            Commands.literal(ROOT_LITERAL)
                    .requires { source ->
                        source.requiresPermission(
                            permissions,
                            CommandPermissions.ROOT
                        )
                    }
                    .then(
                        Commands.literal("status")
                                .executes { context ->
                                    val source = context.source
                                    source.sendSuccess(
                                        {
                                            Messages.prefixed(
                                                "${health.summary()} configGeneration=${config.generation}"
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
                                    val job = kernel.launch {
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
                                                            result.errors.joinToString("; ") {
                                                                "${it.path}: ${it.message}"
                                                            }
                                                        }"
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    if (job == null) {
                                        source.sendFailure(
                                            Messages.prefixed("runtime is shutting down; reload rejected")
                                        )
                                        0
                                    } else {
                                        source.sendSuccess(
                                            { Messages.prefixed("reloading config...") },
                                            false,
                                        )
                                        1
                                    }
                                }
                    )
        )
    }

}
