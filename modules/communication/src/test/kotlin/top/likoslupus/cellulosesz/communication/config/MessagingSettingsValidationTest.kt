package top.likoslupus.cellulosesz.communication.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessagingSettingsValidationTest {

    @Test
    fun `accepts defaults`() {
        assertTrue(MessagingSettingsValidation.validate(MessagingSettings()).isEmpty())
    }

    @Test
    fun `rejects non positive private message limits`() {
        val errors = MessagingSettingsValidation.validate(
            MessagingSettings(
                privateMessages = PrivateMessageSettings(
                    maxMessageLength = 0,
                    maxIgnoredPlayers = -1,
                    replyTimeoutSeconds = 0,
                )
            )
        )

        assertTrue(errors.any { it.path == "messaging.privateMessages.maxMessageLength" })
        assertTrue(errors.any { it.path == "messaging.privateMessages.maxIgnoredPlayers" })
        assertTrue(errors.any { it.path == "messaging.privateMessages.replyTimeoutSeconds" })
    }

    @Test
    fun `rejects non positive mail limits`() {
        val errors = MessagingSettingsValidation.validate(
            MessagingSettings(
                mail = MailSettings(
                    maxMessageLength = 0,
                    maxMessagesPerMailbox = 0,
                    maxSendsPerMinute = 0,
                    maxTemporaryMailSeconds = -5,
                )
            )
        )

        assertTrue(errors.any { it.path == "messaging.mail.maxMessageLength" })
        assertTrue(errors.any { it.path == "messaging.mail.maxMessagesPerMailbox" })
        assertTrue(errors.any { it.path == "messaging.mail.maxSendsPerMinute" })
        assertTrue(errors.any { it.path == "messaging.mail.maxTemporaryMailSeconds" })
    }

    @Test
    fun `rejects non positive helpop and announcement limits`() {
        val errors = MessagingSettingsValidation.validate(
            MessagingSettings(
                helpOp = HelpOpSettings(
                    maxMessageLength = 0,
                    maxMessagesPerMinute = 0
                ),
                announcements = AnnouncementSettings(maxMessageLength = 0),
            )
        )

        assertTrue(errors.any { it.path == "messaging.helpOp.maxMessageLength" })
        assertTrue(errors.any { it.path == "messaging.helpOp.maxMessagesPerMinute" })
        assertTrue(errors.any { it.path == "messaging.announcements.maxMessageLength" })
    }

    @Test
    fun `rejects a negative join quit threshold and blank templates`() {
        val errors = MessagingSettingsValidation.validate(
            MessagingSettings(
                joinQuit = JoinQuitSettings(
                    hideAbovePlayerCount = -1,
                    joinFormat = "  ",
                )
            )
        )

        assertTrue(errors.any { it.path == "messaging.joinQuit.hideAbovePlayerCount" })
        assertTrue(errors.any { it.path == "messaging.joinQuit.joinFormat" })
    }

    @Test
    fun `rejects malformed lifecycle and death templates`() {
        val errors = MessagingSettingsValidation.validate(
            MessagingSettings(
                joinQuit = JoinQuitSettings(joinFormat = "<yellow>oops"),
                death = DeathMessageSettings(
                    customFormat = "<red>oops",
                    coordinatesFormat = "<gray>oops",
                ),
            )
        )

        assertTrue(errors.any { it.path == "messaging.joinQuit.joinFormat" })
        assertTrue(errors.any { it.path == "messaging.death.customFormat" })
        assertTrue(errors.any { it.path == "messaging.death.coordinatesFormat" })
    }

}
