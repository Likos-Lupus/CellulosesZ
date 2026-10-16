package top.likoslupus.cellulosesz.api.communication

/** Groups the Communication domain: private messages, mail, announcements and their events. */
public interface CommunicationApi {

    public val privateMessages: PrivateMessagesApi

    public val mail: MailApi

    public val announcements: AnnouncementsApi

    public val events: CommunicationEvents

}
