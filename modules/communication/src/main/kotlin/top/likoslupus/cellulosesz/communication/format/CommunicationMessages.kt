package top.likoslupus.cellulosesz.communication.format

import top.likoslupus.cellulosesz.communication.SenderRestriction
import top.likoslupus.cellulosesz.communication.action.ActionMessageResult
import top.likoslupus.cellulosesz.communication.announcement.AnnouncementResult
import top.likoslupus.cellulosesz.communication.mail.*
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageOutcome
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.communication.page.ServerPageResult
import top.likoslupus.cellulosesz.communication.preferences.IgnoreUpdateResult
import top.likoslupus.cellulosesz.communication.preferences.PreferenceUpdateResult
import top.likoslupus.cellulosesz.communication.staff.HelpOpResult
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*

/**
 * Maps communication domain results to semantic values. It holds no language and no colour: the
 * presentation layer renders per recipient. User-entered text is only ever carried as [TextArgument.Literal].
 */
internal object CommunicationMessages {

    fun incomingPrivateMessage(
        senderName: String,
        body: MessageBody,
    ): Notice =
        Notice(
            LocalizedText(
                MessageKey("communication.msg.incoming"),
                listOf(playerName(senderName), literal(body.value)),
            ),
            NoticeTone.INFO,
        )

    fun outgoingPrivateMessage(
        targetName: String,
        body: MessageBody,
    ): Notice =
        Notice(
            LocalizedText(
                MessageKey("communication.msg.outgoing"),
                listOf(playerName(targetName), literal(body.value)),
            ),
            NoticeTone.INFO,
        )

    fun senderRestricted(reason: SenderRestriction): Notice =
        when (reason) {
            SenderRestriction.MUTED -> error(MessageKey("communication.msg.sender_muted"))
        }

    fun privateMessageFeedback(outcome: PrivateMessageOutcome): Notice =
        error(
            when (outcome) {
                is PrivateMessageOutcome.Delivered ->
                    return outgoingPrivateMessage(
                        outcome.target.name,
                        outcome.body
                    )

                PrivateMessageOutcome.Disabled ->
                    MessageKey("communication.msg.disabled")

                PrivateMessageOutcome.InvalidMessage ->
                    MessageKey("communication.msg.invalid")

                PrivateMessageOutcome.SenderRequired,
                PrivateMessageOutcome.SenderOffline ->
                    MessageKey("core.error.player_required")

                is PrivateMessageOutcome.SenderRestricted ->
                    senderRestricted(outcome.reason)

                is PrivateMessageOutcome.TargetOffline ->
                    when (val name = outcome.name) {
                        null -> MessageKey("communication.msg.target_offline")
                        else -> return error(
                            MessageKey("communication.msg.target_offline_named"),
                            playerName(name)
                        )
                    }

                PrivateMessageOutcome.SelfTarget ->
                    MessageKey("communication.msg.self")

                PrivateMessageOutcome.NoReplyTarget ->
                    MessageKey("communication.msg.no_reply_target")

                PrivateMessageOutcome.ReplyDisabled ->
                    MessageKey("communication.msg.reply_disabled")

                PrivateMessageOutcome.RecipientUnavailable ->
                    MessageKey("communication.msg.recipient_unavailable")

                PrivateMessageOutcome.RecipientStateLoading ->
                    MessageKey("communication.msg.recipient_loading")
            }
        )

    fun storageUnavailable(): Notice =
        error(MessageKey("communication.preference.storage_unavailable"))

    fun runtimeStopping(): Notice =
        error(MessageKey("core.error.runtime_stopping"))

    // Preferences -----------------------------------------------------------------------------

    fun receiveStatus(enabled: Boolean): Notice =
        info(
            when {
                enabled -> MessageKey("communication.preference.receive_enabled")
                else -> MessageKey("communication.preference.receive_disabled")
            }
        )

    fun receiveUpdated(enabled: Boolean): Notice =
        success(
            when {
                enabled -> MessageKey("communication.preference.receive_updated_on")
                else -> MessageKey("communication.preference.receive_updated_off")
            }
        )

    fun alreadySet(): Notice =
        info(MessageKey("communication.preference.already_set"))

    fun replyStatus(enabled: Boolean): Notice =
        info(
            when {
                enabled -> MessageKey("communication.preference.reply_enabled")
                else -> MessageKey("communication.preference.reply_disabled")
            }
        )

    fun replyUpdated(enabled: Boolean): Notice =
        success(
            when {
                enabled -> MessageKey("communication.preference.reply_updated_on")
                else -> MessageKey("communication.preference.reply_updated_off")
            }
        )

    fun ignoreUpdated(name: String): Notice =
        success(
            MessageKey("communication.preference.ignore_updated"),
            playerName(name)
        )

    fun noLongerIgnoring(name: String): Notice =
        success(
            MessageKey("communication.preference.ignore_removed"),
            playerName(name)
        )

    fun alreadyIgnored(name: String): Notice =
        info(
            MessageKey("communication.preference.ignore_already"),
            playerName(name)
        )

    fun notIgnored(name: String): Notice =
        info(
            MessageKey("communication.preference.ignore_not"),
            playerName(name)
        )

    fun ignoreListFull(): Notice =
        error(MessageKey("communication.preference.ignore_full"))

    fun ignoreList(names: List<String>): Notice =
        when {
            names.isEmpty() -> info(MessageKey("communication.preference.ignore_empty"))
            else -> info(
                MessageKey("communication.preference.ignore_list"),
                literal(names.joinToString(", "))
            )
        }

    fun preferenceFeedback(result: PreferenceUpdateResult): Notice =
        when (result) {
            is PreferenceUpdateResult.Updated ->
                receiveUpdated(result.preferences.receivePrivateMessages)

            PreferenceUpdateResult.AlreadySet -> alreadySet()
            PreferenceUpdateResult.StorageUnavailable -> storageUnavailable()
            PreferenceUpdateResult.RuntimeStopping -> runtimeStopping()
        }

    fun ignoreFeedback(
        result: IgnoreUpdateResult,
        name: String,
    ): Notice =
        when (result) {
            is IgnoreUpdateResult.Updated -> info(MessageKey("communication.preference.ignore_list_updated"))
            IgnoreUpdateResult.AlreadyIgnored -> alreadyIgnored(name)
            IgnoreUpdateResult.NotIgnored -> notIgnored(name)
            IgnoreUpdateResult.IgnoreListFull -> ignoreListFull()
            IgnoreUpdateResult.StorageUnavailable -> storageUnavailable()
            IgnoreUpdateResult.RuntimeStopping -> runtimeStopping()
        }

    // Mail ------------------------------------------------------------------------------------

    fun mailDisabled(): Notice =
        error(MessageKey("communication.mail.disabled"))

    fun mailInvalid(): Notice =
        error(MessageKey("communication.mail.invalid"))

    fun mailSelf(): Notice =
        error(MessageKey("communication.mail.self"))

    fun mailTargetUnknown(name: String): Notice =
        error(
            MessageKey("communication.mail.target_unknown"),
            playerName(name)
        )

    fun mailRateLimited(): Notice =
        error(MessageKey("communication.mail.rate_limited"))

    fun mailSent(name: String): Notice =
        success(MessageKey("communication.mail.sent"), playerName(name))

    fun mailStorageUnavailable(): Notice =
        error(MessageKey("communication.mail.storage_unavailable"))

    fun mailSummary(result: MailSummaryResult): Notice =
        when (result) {
            MailSummaryResult.StorageUnavailable -> mailStorageUnavailable()
            is MailSummaryResult.Summary ->
                when {
                    result.total == 0 -> info(MessageKey("communication.mail.none"))
                    else -> info(
                        MessageKey("communication.mail.summary"),
                        count(result.total),
                        count(result.unread),
                    )
                }
        }

    fun mailSendFeedback(result: MailSendResult): Notice =
        when (result) {
            is MailSendResult.Sent -> mailSent(result.target.name)
            MailSendResult.Disabled -> mailDisabled()
            MailSendResult.InvalidMessage -> mailInvalid()
            is MailSendResult.SenderRestricted -> senderRestricted(result.reason)
            is MailSendResult.TargetUnknown -> mailTargetUnknown(result.name)
            MailSendResult.SelfTarget -> mailSelf()
            MailSendResult.RateLimited -> mailRateLimited()
            MailSendResult.MailboxFull -> error(MessageKey("communication.mail.mailbox_full"))
            MailSendResult.DurationTooLong -> error(MessageKey("communication.mail.duration_too_long"))
            MailSendResult.StorageUnavailable -> mailStorageUnavailable()
        }

    fun mailView(result: MailViewResult): ListPage =
        when (result) {
            is MailViewResult.Loaded -> ListPage(
                title = LocalizedText(MessageKey("communication.mail.read_title")),
                rows = result.page.mapIndexed { offset, message ->
                    ListRow(
                        main = mailLine(
                            index = (result.pageIndex - 1) * MAIL_PAGE_SIZE + offset + 1,
                            message = message,
                        )
                    )
                },
                emptyText = LocalizedText(MessageKey("communication.mail.none")),
                summary = LocalizedText(
                    MessageKey("communication.mail.read_summary"),
                    listOf(
                        count(result.pageIndex),
                        count(result.pageCount),
                        count(result.total),
                        count(result.unread),
                    ),
                ),
            )

            MailViewResult.Empty -> ListPage(
                title = LocalizedText(MessageKey("communication.mail.read_title")),
                rows = emptyList(),
                emptyText = LocalizedText(MessageKey("communication.mail.none")),
            )

            MailViewResult.StorageUnavailable -> ListPage(
                title = LocalizedText(MessageKey("communication.mail.read_title")),
                rows = emptyList(),
                emptyText = LocalizedText(MessageKey("communication.mail.storage_unavailable")),
            )
        }

    fun mailCleared(): Notice =
        success(MessageKey("communication.mail.cleared"))

    fun unreadMailNotice(count: Int): Notice =
        info(
            MessageKey("communication.mail.unread_notice"),
            count(count)
        )

    fun newMailNotice(fromName: String): Notice =
        info(
            MessageKey("communication.mail.new_mail"),
            playerName(fromName)
        )

    // HelpOp ----------------------------------------------------------------------------------

    fun helpOpIncoming(
        senderName: String,
        body: MessageBody,
    ): Notice =
        Notice(
            LocalizedText(
                MessageKey("communication.helpop.incoming"),
                listOf(playerName(senderName), literal(body.value)),
            ),
            NoticeTone.INFO,
        )

    fun helpOpFeedback(result: HelpOpResult): Notice =
        when (result) {
            is HelpOpResult.Delivered -> success(
                MessageKey("communication.helpop.delivered"),
                count(result.recipientCount),
            )

            HelpOpResult.LoggedOnly -> info(MessageKey("communication.helpop.logged_only"))
            HelpOpResult.Disabled -> error(MessageKey("communication.helpop.disabled"))
            HelpOpResult.InvalidMessage -> error(MessageKey("communication.helpop.invalid"))
            HelpOpResult.RateLimited -> error(MessageKey("communication.helpop.rate_limited"))
        }

    // Announcements ---------------------------------------------------------------------------

    fun broadcastLine(body: MessageBody): Notice =
        Notice(
            LocalizedText(
                MessageKey("communication.broadcast.line"),
                listOf(literal(body.value))
            ),
            NoticeTone.INFO,
        )

    fun announcementFeedback(result: AnnouncementResult): Notice =
        when (result) {
            is AnnouncementResult.Delivered -> success(
                MessageKey("communication.broadcast.delivered"),
                count(result.recipientCount),
            )

            AnnouncementResult.NoRecipients ->
                info(MessageKey("communication.broadcast.no_recipients"))

            AnnouncementResult.Disabled ->
                error(MessageKey("communication.broadcast.disabled"))

            AnnouncementResult.InvalidMessage ->
                error(MessageKey("communication.broadcast.invalid"))
        }

    // Action messages -------------------------------------------------------------------------

    fun actionLine(
        senderName: String,
        body: MessageBody,
    ): Notice =
        Notice(
            LocalizedText(
                MessageKey("communication.action.line"),
                listOf(playerName(senderName), literal(body.value)),
            ),
            NoticeTone.INFO,
        )

    fun actionFeedback(result: ActionMessageResult): Notice =
        when (result) {
            is ActionMessageResult.Delivered -> result.notice
            ActionMessageResult.Disabled -> error(MessageKey("communication.action.disabled"))
            ActionMessageResult.InvalidMessage -> error(MessageKey("communication.action.invalid"))
            ActionMessageResult.RateLimited -> error(MessageKey("communication.action.rate_limited"))
            ActionMessageResult.Muted -> error(MessageKey("communication.action.sender_muted"))
        }

    // Server pages ----------------------------------------------------------------------------

    fun pageFeedback(
        result: ServerPageResult,
        pageName: String,
        fixed: Boolean,
    ): Notice =
        when (result) {
            is ServerPageResult.Found -> error(MessageKey("communication.page.invalid"))
            ServerPageResult.Disabled -> error(MessageKey("communication.page.disabled"))
            ServerPageResult.InvalidName -> error(MessageKey("communication.page.invalid_name"))
            ServerPageResult.NoSuchPage -> when {
                fixed -> error(MessageKey("communication.page.missing"), literal(pageName))
                else -> error(MessageKey("communication.page.unknown"), literal(pageName))
            }

            ServerPageResult.RootMissing -> error(MessageKey("communication.page.root_missing"))
            ServerPageResult.TooLarge ->
                error(MessageKey("communication.page.too_large"), literal(pageName))

            ServerPageResult.TooManyPages -> error(MessageKey("communication.page.too_many"))
            ServerPageResult.Invalid ->
                error(MessageKey("communication.page.invalid"), literal(pageName))
        }

    private fun mailLine(
        index: Int,
        message: MailMessage,
    ): LocalizedText {
        val arguments = listOf(
            count(index),
            playerName(message.sender.displayName),
            literal(message.body.value),
        )
        return when (message.readAt) {
            null -> LocalizedText(
                MessageKey("communication.mail.read_line_unread"),
                arguments
            )

            else -> LocalizedText(
                MessageKey("communication.mail.read_line"),
                arguments
            )
        }
    }

    private fun error(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.ERROR
        )

    private fun success(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.SUCCESS
        )

    private fun info(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.INFO
        )

    private fun literal(value: String): TextArgument =
        TextArgument.Literal(value)

    private fun playerName(value: String): TextArgument =
        TextArgument.PlayerName(value)

    private fun count(value: Int): TextArgument =
        TextArgument.Count(value.toLong())

    private const val MAIL_PAGE_SIZE: Int = 10

}
