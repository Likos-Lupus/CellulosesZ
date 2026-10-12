package top.likoslupus.cellulosesz.administration.moderation.notify

import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/**
 * Sends a short moderation notice to every online player holding the moderator capability. The
 * notices are semantic: they are rendered once per recipient so each moderator sees the notice in
 * their own language. Must run on the server thread.
 */
internal class ModerationNotifier(
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
) {

    fun notify(notices: List<Notice>) {
        if (notices.isEmpty()) {
            return
        }
        kernel.requireServer().playerList.players.forEach { player ->
            if (
                player.createCommandSourceStack().requiresPermission(
                    permissions,
                    CommandPermissions.MODERATOR
                )
            ) {
                val audience = adventure.audience(CommandFeedbackTarget.Player(player.uuid))
                notices
                        .flatMap { presentation.renderFor(player.uuid, it) }
                        .forEach(audience::sendMessage)
            }
        }
    }

}
