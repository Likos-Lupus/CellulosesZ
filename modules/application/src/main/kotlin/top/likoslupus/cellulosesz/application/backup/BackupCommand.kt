package top.likoslupus.cellulosesz.application.backup

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages

/** `/backup` — runs the configured external backup command once and reports its exit status. */
internal object BackupCommand {

    fun definition(
        kernel: RuntimeKernel,
        service: BackupService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): CommandDefinition =
        command(
            name = "backup",
            category = CommandCategory.ROOT,
            permission = CommandPermissions.BACKUP,
            documentation = "core/backup",
        ) {
            executes {
                val playerId = source.player?.uuid
                val trigger = source.textName
                kernel.launch {
                    val outcome = service.runBackup(trigger)
                    kernel.onServerThread {
                        source.reply(
                            adventure,
                            messages.renderFor(
                                playerId,
                                outcome.key(),
                                outcome.arguments()
                            )
                        )
                    }
                }
                1
            }
        }

}

private fun BackupOutcome.key(): MessageKey =
    when (this) {
        BackupOutcome.Disabled -> MessageKey("core.backup.disabled")
        BackupOutcome.NotConfigured -> MessageKey("core.backup.not_configured")
        BackupOutcome.AlreadyRunning -> MessageKey("core.backup.already_running")
        is BackupOutcome.Completed -> MessageKey("core.backup.completed")
        BackupOutcome.TimedOut -> MessageKey("core.backup.timed_out")
        is BackupOutcome.Failed -> MessageKey("core.backup.failed")
    }

private fun BackupOutcome.arguments(): List<MessageArgument> =
    when (this) {
        is BackupOutcome.Completed -> listOf(MessageArgument(exitCode.toString()))
        else -> emptyList()
    }
