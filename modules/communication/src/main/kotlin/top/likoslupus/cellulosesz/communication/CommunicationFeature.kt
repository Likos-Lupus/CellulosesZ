package top.likoslupus.cellulosesz.communication

import top.likoslupus.cellulosesz.communication.announcement.AnnouncementCommands
import top.likoslupus.cellulosesz.communication.announcement.AnnouncementService
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.mail.JdbcMailboxRepository
import top.likoslupus.cellulosesz.communication.mail.MailRateLimiter
import top.likoslupus.cellulosesz.communication.mail.MailService
import top.likoslupus.cellulosesz.communication.mail.MailSummaryResult
import top.likoslupus.cellulosesz.communication.mail.command.MailCommands
import top.likoslupus.cellulosesz.communication.messaging.MinecraftMessagingBackend
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageService
import top.likoslupus.cellulosesz.communication.messaging.ReplyState
import top.likoslupus.cellulosesz.communication.messaging.command.PrivateMessageCommands
import top.likoslupus.cellulosesz.communication.preferences.JdbcMessagingPreferencesRepository
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferencesService
import top.likoslupus.cellulosesz.communication.preferences.command.MessagingPreferenceCommands
import top.likoslupus.cellulosesz.communication.staff.HelpOpCommands
import top.likoslupus.cellulosesz.communication.staff.HelpOpService
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.runtime.serverThreadRunner
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import java.time.Clock
import java.util.*
import kotlin.time.Duration.Companion.seconds

/** Holds the composed integration, set at command-registration time. */
internal class IntegrationHolder {

    @Volatile var value: CommunicationIntegration = CommunicationIntegration()

}

/**
 * Public surface of the communication bounded context. Internals stay `internal`.
 */
public class CommunicationFeature internal constructor(
    private val messages: PrivateMessageService,
    private val preferences: MessagingPreferencesService,
    private val mail: MailService,
    private val helpOp: HelpOpService,
    private val announcements: AnnouncementService,
    private val replyState: ReplyState,
    private val holder: IntegrationHolder,
    private val kernel: RuntimeKernel,
    private val known: MinecraftKnownPlayerResolver,
    private val settings: () -> MessagingSettings,
) {

    public fun commands(integration: CommunicationIntegration): List<CommandDefinition> {
        holder.value = integration
        return PrivateMessageCommands.commands(messages) +
                MessagingPreferenceCommands.commands(preferences, known, kernel) +
                MailCommands.commands(mail, kernel) +
                HelpOpCommands.commands(helpOp, kernel) +
                AnnouncementCommands.commands(announcements, kernel)
    }

    public fun onServerStarting() {
        // All state is per-player and lazily loaded; there is nothing to scan at startup.
    }

    public fun onPlayerJoined(playerId: UUID) {
        preferences.beginLoad(playerId)
        kernel.launch {
            preferences.load(playerId)
            notifyUnreadMail(playerId)
        }
    }

    public fun onPlayerQuit(playerId: UUID) {
        preferences.clear(playerId)
        replyState.clear(playerId)
    }

    public fun onServerStopping() {
        preferences.shutdown()
    }

    private suspend fun notifyUnreadMail(playerId: UUID) {
        if (!settings().mail.notifyOnJoin) {
            return
        }

        val identity = kernel.onServerThread {
            kernel.requireServer().playerList.getPlayer(playerId)
                    ?.let { KnownPlayerIdentity(it.uuid, it.gameProfile.name) }
        } ?: return

        when (val summary = mail.summary(identity)) {
            is MailSummaryResult.Summary ->
                if (summary.unread > 0) {
                    kernel.messagePlayer(
                        playerId,
                        CommunicationMessages.unreadMailNotice(summary.unread)
                    )
                }

            MailSummaryResult.StorageUnavailable -> Unit
        }
    }

}

public fun createCommunicationFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    permissions: PermissionService,
    settings: () -> MessagingSettings,
    known: MinecraftKnownPlayerResolver,
): CommunicationFeature {
    val holder = IntegrationHolder()
    val runner = kernel.serverThreadRunner()

    val preferencesRepository = JdbcMessagingPreferencesRepository(database, namespace)
    val preferences = MessagingPreferencesService(
        runner = runner,
        repository = preferencesRepository,
        settings = { settings().privateMessages },
    )
    val replyState = ReplyState(
        timeout = { settings().privateMessages.replyTimeoutSeconds?.seconds }
    )
    val messages = PrivateMessageService(
        backend = MinecraftMessagingBackend(kernel),
        recipients = preferences,
        replyState = replyState,
        settings = { settings().privateMessages },
        integration = { holder.value },
    )

    val mail = MailService(
        runner = runner,
        mailboxes = JdbcMailboxRepository(database, namespace),
        preferences = preferencesRepository,
        identities = known,
        senderGate = { holder.value.senderGate },
        rateLimiter = MailRateLimiter(),
        clock = Clock.systemUTC(),
        settings = { settings().mail },
        locks = KeyedMutex(),
    )

    val directory = MinecraftPlayerDirectory(kernel, permissions)
    val helpOp = HelpOpService(
        runner = runner,
        directory = directory,
        settings = { settings().helpOp },
    )
    val announcements = AnnouncementService(
        runner = runner,
        directory = directory,
        settings = { settings().announcements },
    )

    return CommunicationFeature(
        messages = messages,
        preferences = preferences,
        mail = mail,
        helpOp = helpOp,
        announcements = announcements,
        replyState = replyState,
        holder = holder,
        kernel = kernel,
        known = known,
        settings = settings,
    )
}
