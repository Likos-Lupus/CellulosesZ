package top.likoslupus.cellulosesz.communication.format

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.communication.announcement.AnnouncementResult
import top.likoslupus.cellulosesz.communication.mail.*
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageOutcome
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.communication.preferences.IgnoreUpdateResult
import top.likoslupus.cellulosesz.communication.preferences.PreferenceUpdateResult
import top.likoslupus.cellulosesz.communication.staff.HelpOpResult
import top.likoslupus.cellulosesz.core.text.Messages

/**
 * Owns every user-facing communication string. Deliberately not a keyed template registry: user
 * text is literal, and this project has no localization framework.
 */
internal object CommunicationMessages {

    fun prefixed(message: String): Component =
        Messages.prefixed(message)

    fun incomingPrivateMessage(
        senderName: String,
        body: MessageBody,
    ): Component =
        Component.literal("[$senderName -> you] ${body.value}")

    fun outgoingPrivateMessage(
        targetName: String,
        body: MessageBody,
    ): Component =
        Component.literal("[you -> $targetName] ${body.value}")

    fun privateMessageFeedback(outcome: PrivateMessageOutcome): Component =
        prefixed(
            when (outcome) {
                is PrivateMessageOutcome.Delivered ->
                    return outgoingPrivateMessage(outcome.target.name, outcome.body)

                PrivateMessageOutcome.Disabled ->
                    "private messaging is disabled"

                PrivateMessageOutcome.InvalidMessage ->
                    "your message is empty or too long"

                PrivateMessageOutcome.SenderRequired,
                PrivateMessageOutcome.SenderOffline ->
                    "this command requires a player"

                is PrivateMessageOutcome.SenderRestricted ->
                    return outcome.feedback

                is PrivateMessageOutcome.TargetOffline ->
                    when (val name = outcome.name) {
                        null -> "that player is not online"
                        else -> "player '$name' is not online"
                    }

                PrivateMessageOutcome.SelfTarget ->
                    "you cannot message yourself"

                PrivateMessageOutcome.NoReplyTarget ->
                    "you have no one to reply to"

                PrivateMessageOutcome.RecipientUnavailable ->
                    "that player is not accepting private messages"

                PrivateMessageOutcome.RecipientStateLoading ->
                    "that player's messaging state is still loading"
            }
        )

    fun requiresPlayer(): Component =
        prefixed("this command requires a player")

    fun storageUnavailable(): Component =
        prefixed("communication storage is unavailable")

    fun runtimeStopping(): Component =
        prefixed("runtime is shutting down")

    // Preferences -----------------------------------------------------------------------------

    fun receiveStatus(enabled: Boolean): Component =
        prefixed(
            when {
                enabled -> "private messages are enabled"
                else -> "private messages are disabled"
            }
        )

    fun receiveUpdated(enabled: Boolean): Component =
        prefixed(
            when {
                enabled -> "you will now receive private messages"
                else -> "you will no longer receive private messages"
            }
        )

    fun alreadySet(): Component =
        prefixed("that setting is already applied")

    fun ignoreUpdated(name: String): Component =
        prefixed("now ignoring $name")

    fun noLongerIgnoring(name: String): Component =
        prefixed("no longer ignoring $name")

    fun alreadyIgnored(name: String): Component =
        prefixed("$name is already ignored")

    fun notIgnored(name: String): Component =
        prefixed("$name is not ignored")

    fun ignoreListFull(): Component =
        prefixed("your ignore list is full")

    fun ignoreList(names: List<String>): Component =
        prefixed(
            when {
                names.isEmpty() -> "you are not ignoring anyone"
                else -> "ignored: ${names.joinToString(", ")}"
            }
        )

    fun preferenceFeedback(result: PreferenceUpdateResult): Component =
        when (result) {
            is PreferenceUpdateResult.Updated ->
                receiveUpdated(result.preferences.receivePrivateMessages)

            PreferenceUpdateResult.AlreadySet -> alreadySet()
            PreferenceUpdateResult.StorageUnavailable -> storageUnavailable()
            PreferenceUpdateResult.RuntimeStopping -> runtimeStopping()
        }

    fun ignoreFeedback(result: IgnoreUpdateResult): Component =
        prefixed(
            when (result) {
                is IgnoreUpdateResult.Updated -> "ignore list updated"
                IgnoreUpdateResult.AlreadyIgnored -> "that player is already ignored"
                IgnoreUpdateResult.NotIgnored -> "that player is not ignored"
                IgnoreUpdateResult.IgnoreListFull -> return ignoreListFull()
                IgnoreUpdateResult.StorageUnavailable -> return storageUnavailable()
                IgnoreUpdateResult.RuntimeStopping -> return runtimeStopping()
            }
        )

    // Mail ------------------------------------------------------------------------------------

    fun mailDisabled(): Component =
        prefixed("mail is disabled")

    fun mailInvalid(): Component =
        prefixed("your mail is empty or too long")

    fun mailSelf(): Component =
        prefixed("you cannot mail yourself")

    fun mailTargetUnknown(name: String): Component =
        prefixed("player '$name' is not known to this server")

    fun mailRateLimited(): Component =
        prefixed("you are sending mail too quickly")

    fun mailSent(name: String): Component =
        prefixed("mail sent to $name")

    fun mailStorageUnavailable(): Component =
        prefixed("mail storage is unavailable")

    fun mailSummary(result: MailSummaryResult): Component =
        when (result) {
            MailSummaryResult.StorageUnavailable -> mailStorageUnavailable()
            is MailSummaryResult.Summary ->
                when {
                    result.total == 0 -> prefixed("you have no mail")
                    else -> prefixed(
                        "you have ${result.total} mail message(s), ${result.unread} unread. " +
                                "Use /mail read to view them."
                    )
                }
        }

    fun mailSendFeedback(result: MailSendResult): Component =
        when (result) {
            is MailSendResult.Sent -> mailSent(result.target.name)
            MailSendResult.Disabled -> mailDisabled()
            MailSendResult.InvalidMessage -> mailInvalid()
            is MailSendResult.SenderRestricted -> result.feedback
            is MailSendResult.TargetUnknown -> mailTargetUnknown(result.name)
            MailSendResult.SelfTarget -> mailSelf()
            MailSendResult.RateLimited -> mailRateLimited()
            MailSendResult.MailboxFull -> prefixed("that player's mailbox is full")
            MailSendResult.DurationTooLong -> prefixed("temporary mail duration exceeds the configured maximum")
            MailSendResult.StorageUnavailable -> mailStorageUnavailable()
        }

    fun mailEmpty(): Component =
        prefixed("you have no mail")

    fun mailCleared(): Component =
        prefixed("your mail has been cleared")

    fun mailHeader(view: MailViewResult.Loaded): Component =
        prefixed(
            "mail page ${view.pageIndex}/${view.pageCount} " +
                    "(${view.total} total, ${view.unread} unread)"
        )

    fun mailLine(index: Int, message: MailMessage): Component {
        val flag = when (message.readAt) {
            null -> " (unread)"
            else -> ""
        }
        return prefixed("#$index from ${message.sender.displayName}$flag: ${message.body.value}")
    }

    fun unreadMailNotice(count: Int): Component =
        prefixed("you have $count unread mail message(s); use /mail read")

    fun newMailNotice(fromName: String): Component =
        prefixed("you have new mail from $fromName")

    // HelpOp ----------------------------------------------------------------------------------

    fun helpOpIncoming(
        senderName: String,
        body: MessageBody,
    ): Component =
        Component.literal("[HelpOp] $senderName: ${body.value}")

    fun helpOpFeedback(result: HelpOpResult): Component =
        prefixed(
            when (result) {
                is HelpOpResult.Delivered -> "your message was sent to ${result.recipientCount} moderator(s)"
                HelpOpResult.LoggedOnly -> "no moderators are online; your message was logged"
                HelpOpResult.Disabled -> "helpop is disabled"
                HelpOpResult.InvalidMessage -> "your message is empty or too long"
                HelpOpResult.RateLimited -> "you are sending help requests too quickly"
            }
        )

    // Announcements ---------------------------------------------------------------------------

    fun broadcastLine(body: MessageBody): Component =
        Component.literal("[Broadcast] ${body.value}")

    fun announcementFeedback(result: AnnouncementResult): Component =
        prefixed(
            when (result) {
                is AnnouncementResult.Delivered -> "announcement sent to ${result.recipientCount} player(s)"
                AnnouncementResult.NoRecipients -> "no players received the announcement"
                AnnouncementResult.Disabled -> "announcements are disabled"
                AnnouncementResult.InvalidMessage -> "your announcement is empty or too long"
            }
        )

}
