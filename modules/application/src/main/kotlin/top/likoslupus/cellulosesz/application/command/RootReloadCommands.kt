package top.likoslupus.cellulosesz.application.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.application.config.CellulosesConfig
import top.likoslupus.cellulosesz.core.command.dsl.CommandSyntax
import top.likoslupus.cellulosesz.core.command.feedbackTarget
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.message
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.foundation.config.ConfigReloadResult
import top.likoslupus.cellulosesz.foundation.config.ConfigStore

/** `/cellulosesz reload`. */
internal fun CommandSyntax.reloadCommand(
    config: ConfigStore<CellulosesConfig>,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    onReloadApplied: suspend () -> Unit,
) {
    literal("reload", permission = CommandPermissions.ROOT_RELOAD) {
        executes {
            reload(
                source,
                config,
                kernel,
                messages,
                adventure,
                onReloadApplied
            )
        }
    }
}

private fun reload(
    source: CommandSourceStack,
    config: ConfigStore<CellulosesConfig>,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    onReloadApplied: suspend () -> Unit,
): Int {
    val playerId = source.player?.uuid
    val target = source.feedbackTarget()
    val job = kernel.launch {
        when (val result = config.reload()) {
            is ConfigReloadResult.Success -> {
                onReloadApplied()
                kernel.message(
                    target,
                    adventure,
                    messages.renderFor(
                        playerId,
                        MessageKey("core.reload.succeeded"),
                        listOf(MessageArgument(result.generation.toString())),
                    ),
                )
            }

            is ConfigReloadResult.Failure -> kernel.message(
                target,
                adventure,
                messages.renderFor(
                    playerId,
                    MessageKey("core.reload.failed"),
                    listOf(
                        MessageArgument(
                            result.errors.joinToString("; ") {
                                "${it.path}: ${it.message}"
                            }
                        )
                    ),
                ),
            )
        }
    }
    return when (job) {
        null -> source.replyError(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.error.runtime_stopping")
            ),
        )

        else -> source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.reload.pending")
            )
        )
    }
}
