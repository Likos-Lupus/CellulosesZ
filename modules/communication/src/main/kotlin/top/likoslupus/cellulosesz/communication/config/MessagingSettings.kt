package top.likoslupus.cellulosesz.communication.config

import kotlinx.serialization.Serializable

/** Root of the communication bounded context's configuration. */
@Serializable
public data class MessagingSettings(
    public val enabled: Boolean = true,
    public val privateMessages: PrivateMessageSettings = PrivateMessageSettings(),
    public val mail: MailSettings = MailSettings(),
    public val helpOp: HelpOpSettings = HelpOpSettings(),
    public val announcements: AnnouncementSettings = AnnouncementSettings(),
)
