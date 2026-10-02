package top.likoslupus.cellulosesz.administration.operator

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.core.text.Messages

/** Feedback text for operator player control. */
internal object PlayerControlFeedback {

    fun kill(result: PlayerControlResult): Component =
        prefixed(
            when (result) {
                is PlayerControlResult.Killed -> "killed ${result.target.name}${auditNote(result.auditRecorded)}"
                PlayerControlResult.TargetOffline -> "that player is not online"
                PlayerControlResult.SelfTarget -> "you cannot target yourself"
                PlayerControlResult.TargetProtected -> "that player is protected from this action"
                PlayerControlResult.RuntimeStopping -> "runtime is shutting down"
                else -> "unexpected result"
            }
        )

    fun gameMode(result: PlayerControlResult): Component =
        prefixed(
            when (result) {
                is PlayerControlResult.GameModeChanged -> "set ${result.target.name} " +
                        "to ${result.mode.name.lowercase()}" +
                        auditNote(result.auditRecorded)

                PlayerControlResult.TargetOffline -> "that player is not online"
                PlayerControlResult.TargetProtected -> "that player is protected from this action"
                PlayerControlResult.RuntimeStopping -> "runtime is shutting down"
                else -> "unexpected result"
            }
        )

    fun sudo(result: PlayerControlResult): Component =
        prefixed(
            when (result) {
                is PlayerControlResult.SudoExecuted -> "executed command as " +
                        "${result.target.name}${auditNote(result.auditRecorded)}"

                PlayerControlResult.TargetOffline -> "that player is not online"
                PlayerControlResult.SelfTarget -> "you cannot target yourself"
                PlayerControlResult.TargetProtected -> "that player is protected from this action"
                PlayerControlResult.RuntimeStopping -> "runtime is shutting down"
                else -> "unexpected result"
            }
        )

    private fun auditNote(recorded: Boolean): String =
        when {
            recorded -> ""
            else -> " (audit record could not be written)"
        }

    private fun prefixed(message: String): Component =
        Messages.prefixed(message)

}
