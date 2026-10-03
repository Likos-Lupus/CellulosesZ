package top.likoslupus.cellulosesz.communication.config

import top.likoslupus.cellulosesz.foundation.config.ValidationError

/**
 * Communication-owned configuration rules. The application aggregates these errors and attaches
 * them to the root schema path instead of re-implementing feature validation.
 */
public object MessagingSettingsValidation {

    public fun validate(settings: MessagingSettings): List<ValidationError> =
        buildList {
            val private = settings.privateMessages
            if (private.maxMessageLength <= 0) {
                add(
                    ValidationError(
                        "messaging.privateMessages.maxMessageLength",
                        "must be > 0",
                    )
                )
            }
            if (private.maxIgnoredPlayers <= 0) {
                add(
                    ValidationError(
                        "messaging.privateMessages.maxIgnoredPlayers",
                        "must be > 0",
                    )
                )
            }
            private.replyTimeoutSeconds?.let { seconds ->
                if (seconds <= 0) {
                    add(
                        ValidationError(
                            "messaging.privateMessages.replyTimeoutSeconds",
                            "must be > 0 when set",
                        )
                    )
                }
            }

            val mail = settings.mail
            if (mail.maxMessageLength <= 0) {
                add(
                    ValidationError(
                        "messaging.mail.maxMessageLength",
                        "must be > 0",
                    )
                )
            }
            if (mail.maxMessagesPerMailbox <= 0) {
                add(
                    ValidationError(
                        "messaging.mail.maxMessagesPerMailbox",
                        "must be > 0",
                    )
                )
            }
            if (mail.maxSendsPerMinute <= 0) {
                add(
                    ValidationError(
                        "messaging.mail.maxSendsPerMinute",
                        "must be > 0",
                    )
                )
            }
            mail.maxTemporaryMailSeconds?.let { seconds ->
                if (seconds <= 0) {
                    add(
                        ValidationError(
                            "messaging.mail.maxTemporaryMailSeconds",
                            "must be > 0 when set",
                        )
                    )
                }
            }

            val helpOp = settings.helpOp
            if (helpOp.maxMessageLength <= 0) {
                add(
                    ValidationError(
                        "messaging.helpOp.maxMessageLength",
                        "must be > 0",
                    )
                )
            }
            if (helpOp.maxMessagesPerMinute <= 0) {
                add(
                    ValidationError(
                        "messaging.helpOp.maxMessagesPerMinute",
                        "must be > 0",
                    )
                )
            }

            if (settings.announcements.maxMessageLength <= 0) {
                add(
                    ValidationError(
                        "messaging.announcements.maxMessageLength",
                        "must be > 0",
                    )
                )
            }
        }

}
