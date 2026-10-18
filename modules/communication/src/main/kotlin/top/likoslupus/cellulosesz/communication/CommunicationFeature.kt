package top.likoslupus.cellulosesz.communication

import top.likoslupus.cellulosesz.communication.action.ActionMessageService
import top.likoslupus.cellulosesz.communication.action.MeOverrideState
import top.likoslupus.cellulosesz.communication.action.command.ActionMessageCommands
import top.likoslupus.cellulosesz.communication.announcement.AnnouncementCommands
import top.likoslupus.cellulosesz.communication.announcement.AnnouncementService
import top.likoslupus.cellulosesz.communication.config.MessagingSettings
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.integration.CommunicationIntegrationPort
import top.likoslupus.cellulosesz.communication.integration.DefaultCommunicationIntegrationPort
import top.likoslupus.cellulosesz.communication.lifecycle.DeathMessageService
import top.likoslupus.cellulosesz.communication.lifecycle.LifecycleMessageService
import top.likoslupus.cellulosesz.communication.mail.JdbcMailboxRepository
import top.likoslupus.cellulosesz.communication.mail.MailRateLimiter
import top.likoslupus.cellulosesz.communication.mail.MailService
import top.likoslupus.cellulosesz.communication.mail.MailSummaryResult
import top.likoslupus.cellulosesz.communication.mail.command.MailCommands
import top.likoslupus.cellulosesz.communication.messaging.MinecraftMessagingBackend
import top.likoslupus.cellulosesz.communication.messaging.PrivateMessageService
import top.likoslupus.cellulosesz.communication.messaging.ReplyState
import top.likoslupus.cellulosesz.communication.messaging.command.PrivateMessageCommands
import top.likoslupus.cellulosesz.communication.page.ServerPageRepository
import top.likoslupus.cellulosesz.communication.page.ServerPageResult
import top.likoslupus.cellulosesz.communication.page.ServerPageService
import top.likoslupus.cellulosesz.communication.page.command.ServerPageCommands
import top.likoslupus.cellulosesz.communication.preferences.JdbcMessagingPreferencesRepository
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferencesService
import top.likoslupus.cellulosesz.communication.preferences.command.MessagingPreferenceCommands
import top.likoslupus.cellulosesz.communication.staff.HelpOpCommands
import top.likoslupus.cellulosesz.communication.staff.HelpOpService
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.runtime.serverThreadRunner
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.InformationOutput
import top.likoslupus.cellulosesz.core.text.document.AdventureDocumentRenderer
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import java.nio.file.Path
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
    private val privateMessages: PrivateMessageService,
    private val preferences: MessagingPreferencesService,
    private val mail: MailService,
    private val helpOp: HelpOpService,
    private val announcements: AnnouncementService,
    private val serverPages: ServerPageService,
    private val actionMessages: ActionMessageService,
    private val replyState: ReplyState,
    private val holder: IntegrationHolder,
    private val kernel: RuntimeKernel,
    private val known: MinecraftKnownPlayerResolver,
    private val settings: () -> MessagingSettings,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
    private val languages: LanguageResolver,
    private val documentTheme: () -> DocumentTheme,
    private val playerPreferences: PlayerPreferenceService,
    private val lifecycleMessages: LifecycleMessageService,
    private val deathMessages: DeathMessageService,
) {

    /**
     * The addon-facing integration seam. It exposes only domain operations; capability
     * authorization and the public API projection are the application adapter's responsibility.
     */
    public val integration: CommunicationIntegrationPort =
        DefaultCommunicationIntegrationPort(
            runner = kernel.serverThreadRunner(),
            known = known,
            privateMessages = privateMessages,
            preferences = preferences,
            mail = mail,
            announcements = announcements,
        )

    public fun commands(integration: CommunicationIntegration): List<CommandDefinition> {
        holder.value = integration
        return PrivateMessageCommands.commands(
            privateMessages,
            playerPreferences,
            kernel,
            messages,
            adventure,
            presentation
        ) + MessagingPreferenceCommands.commands(
            preferences,
            known,
            kernel,
            messages,
            adventure,
            presentation
        ) + MailCommands.commands(
            mail,
            kernel,
            messages,
            adventure,
            presentation
        ) + HelpOpCommands.commands(
            helpOp,
            kernel,
            messages,
            adventure,
            presentation
        ) + AnnouncementCommands.commands(
            announcements,
            kernel,
            messages,
            adventure,
            presentation
        ) + ServerPageCommands.commands(
            serverPages,
            languages,
            documentTheme,
            kernel,
            messages,
            adventure,
            presentation
        ) + ActionMessageCommands.commands(
            actionMessages,
            kernel,
            messages,
            adventure,
            presentation,
            settings().actionMessage.enabled
        )
    }

    public fun onServerStarting() {
        lifecycleMessages.install()
        deathMessages.install()
    }

    /**
     * Publishes the startup-bound `/me` takeover decision for the mixin, which reads it while vanilla
     * registers its commands. `true` cancels the vanilla `/me`; `false` never touches it. Call this
     * from the composition root before the server builds its command dispatcher.
     */
    public fun applyVanillaMeOverride(takeOver: Boolean) {
        MeOverrideState.set(takeOver)
    }

    public fun onPlayerJoined(playerId: UUID) {
        preferences.beginLoad(playerId)
        kernel.launch {
            preferences.load(playerId)
            notifyUnreadMail(playerId)
            showMotdOnJoin(playerId)
        }
    }

    public fun onPlayerQuit(playerId: UUID) {
        preferences.clear(playerId)
        replyState.clear(playerId)
    }

    public fun onServerStopping() {
        lifecycleMessages.uninstall()
        deathMessages.uninstall()
        preferences.shutdown()
    }

    private suspend fun showMotdOnJoin(playerId: UUID) {
        val pages = settings().serverPages
        if (!pages.enabled || !pages.showMotdOnJoin) {
            return
        }

        when (val result = serverPages.load("motd", languages.effective(playerId))) {
            is ServerPageResult.Found -> {
                val lines = AdventureDocumentRenderer(documentTheme()).render(result.document)
                InformationOutput(adventure, kernel).deliver(
                    CommandFeedbackTarget.Player(playerId),
                    lines,
                )
            }

            else -> Unit
        }
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
                    kernel.sendNotices(
                        CommandFeedbackTarget.Player(playerId),
                        adventure,
                        presentation,
                        playerId,
                        listOf(CommunicationMessages.unreadMailNotice(summary.unread)),
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
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    configDirectory: Path,
    languages: LanguageResolver,
    documentTheme: () -> DocumentTheme,
    playerPreferences: PlayerPreferenceService,
    groupResolver: (UUID) -> String?,
    onlineCount: () -> Int,
    uniqueCount: () -> Int,
    uptime: () -> String,
    silentJoinVanish: (UUID) -> Unit,
    hiddenJoinQuit: (UUID) -> Boolean,
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
    val privateMessages = PrivateMessageService(
        backend = MinecraftMessagingBackend(kernel, adventure),
        recipients = preferences,
        replyState = replyState,
        settings = { settings().privateMessages },
        integration = { holder.value },
        presentation = presentation,
        adventure = adventure,
        replyToLastRecipient = { playerId ->
            playerPreferences.isEnabled(playerId, PlayerPreference.REPLY_TO_LAST_RECIPIENT)
        },
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

    val directory = MinecraftPlayerDirectory(kernel, permissions, adventure)
    val helpOp = HelpOpService(
        runner = runner,
        directory = directory,
        presentation = presentation,
        settings = { settings().helpOp },
    )
    val announcements = AnnouncementService(
        runner = runner,
        directory = directory,
        presentation = presentation,
        settings = { settings().announcements },
    )
    val serverPages = ServerPageService(
        repository = ServerPageRepository(configDirectory) { settings().serverPages },
        settings = { settings().serverPages },
    )
    val actionMessages = ActionMessageService(
        runner = runner,
        directory = directory,
        presentation = presentation,
        recipients = preferences,
        integration = { holder.value },
        settings = { settings().actionMessage },
    )
    val lifecycleMessages = LifecycleMessageService(
        kernel = kernel,
        settings = settings,
        permissions = permissions,
        known = known,
        adventure = adventure,
        groupResolver = groupResolver,
        onlineCount = onlineCount,
        uniqueCount = uniqueCount,
        uptime = uptime,
        silentJoinVanish = silentJoinVanish,
        hiddenJoinQuit = hiddenJoinQuit,
    )
    val deathMessages = DeathMessageService(
        kernel = kernel,
        settings = settings,
        adventure = adventure,
        presentation = presentation,
    )

    return CommunicationFeature(
        privateMessages = privateMessages,
        preferences = preferences,
        mail = mail,
        helpOp = helpOp,
        announcements = announcements,
        serverPages = serverPages,
        actionMessages = actionMessages,
        replyState = replyState,
        holder = holder,
        kernel = kernel,
        known = known,
        settings = settings,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
        languages = languages,
        documentTheme = documentTheme,
        playerPreferences = playerPreferences,
        lifecycleMessages = lifecycleMessages,
        deathMessages = deathMessages,
    )
}
