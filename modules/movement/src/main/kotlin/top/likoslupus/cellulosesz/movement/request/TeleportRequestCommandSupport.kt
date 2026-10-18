package top.likoslupus.cellulosesz.movement.request

import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import java.util.UUID

/** Helpers shared by more than one teleport request command group. */
internal object TeleportRequestCommandSupport {

    /** Renders [notice] for the single player [targetId], independent of the executing source. */
    fun notify(
        targetId: UUID,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        notice: Notice,
    ) {
        val audience = adventure.audience(CommandFeedbackTarget.Player(targetId))
        presentation.renderFor(targetId, notice)
                .forEach(audience::sendMessage)
    }

}
