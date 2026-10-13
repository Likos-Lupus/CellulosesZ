package top.likoslupus.cellulosesz.communication.action

import top.likoslupus.cellulosesz.core.text.model.Notice

/** The outcome of a `/me` action message. Expected failures are typed, never exceptions. */
internal sealed interface ActionMessageResult {

    /**
     * The action line was broadcast. [notice] is the semantic line, so a console sender that is not
     * part of the audience can still be shown it.
     */
    data class Delivered(val notice: Notice, val recipientCount: Int) : ActionMessageResult

    data object Disabled : ActionMessageResult

    data object InvalidMessage : ActionMessageResult

    data object RateLimited : ActionMessageResult

    /** The sender is muted by administration and may not originate action messages. */
    data object Muted : ActionMessageResult

}
