package top.likoslupus.cellulosesz.administration.moderation.notify

import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages

/** Sends a short moderation notice to every online player holding the moderator capability. */
internal class ModerationNotifier(
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
) {

    fun notify(message: String) {
        kernel.requireServer().playerList.players.forEach { player ->
            if (
                player.createCommandSourceStack().requiresPermission(
                    permissions,
                    CommandPermissions.MODERATOR
                )
            ) {
                player.sendSystemMessage(Messages.prefixed(message))
            }
        }
    }

}
