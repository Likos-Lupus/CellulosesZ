package top.likoslupus.cellulosesz.administration

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.administration.moderation.audit.JdbcModerationAuditRepository
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.ban.BanCommands
import top.likoslupus.cellulosesz.administration.moderation.ban.BanService
import top.likoslupus.cellulosesz.administration.moderation.ban.IpBanCommands
import top.likoslupus.cellulosesz.administration.moderation.ban.MinecraftBanBackend
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.kick.KickCommands
import top.likoslupus.cellulosesz.administration.moderation.kick.KickService
import top.likoslupus.cellulosesz.administration.moderation.mute.JdbcMuteRepository
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteCommands
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteService
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.administration.operator.MinecraftPlayerControlBackend
import top.likoslupus.cellulosesz.administration.operator.PlayerControlCommands
import top.likoslupus.cellulosesz.administration.operator.PlayerControlService
import top.likoslupus.cellulosesz.administration.playerstate.PlayerStateCommands
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyCommands
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyService
import top.likoslupus.cellulosesz.administration.vanish.VanishCommands
import top.likoslupus.cellulosesz.administration.vanish.VanishService
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.message
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.time.Clock
import java.util.*

/**
 * Public surface of the administration bounded context. Internals stay `internal`.
 */
public class AdministrationFeature internal constructor(
    private val kick: KickService,
    private val bans: BanService,
    private val mutes: MuteService,
    private val control: PlayerControlService,
    private val spies: SocialSpyService,
    private val vanish: VanishService,
    private val known: MinecraftKnownPlayerResolver,
    private val notifier: ModerationNotifier,
    private val settings: () -> AdministrationSettings,
    private val kernel: RuntimeKernel,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
) {

    public fun commands(): List<CommandDefinition> {
        val moderation = { settings().moderation }
        return KickCommands.commands(
            kick,
            notifier,
            moderation,
            kernel,
            messages,
            adventure,
            presentation
        ) + BanCommands.commands(
            bans,
            notifier,
            moderation,
            kernel,
            messages,
            adventure,
            presentation
        ) + IpBanCommands.commands(
            bans,
            notifier,
            moderation,
            kernel,
            messages,
            adventure,
            presentation
        ) + MuteCommands.commands(
            mutes,
            notifier,
            moderation,
            kernel,
            messages,
            adventure,
            presentation
        ) + PlayerControlCommands.commands(
            control,
            kernel,
            messages,
            adventure,
            presentation
        ) + SocialSpyCommands.commands(
            spies,
            kernel,
            messages,
            adventure,
            presentation
        ) + VanishCommands.commands(
            vanish,
            kernel,
            messages,
            adventure,
            presentation
        ) + PlayerStateCommands.commands(
            messages,
            adventure,
            presentation
        )
    }

    /** Localized notice shown to a muted player whose chat is blocked. */
    public fun mutedChatNotice(): Notice =
        AdministrationMessages.mutedChat()

    /** Seeds known identities and loads durable mutes. Must run when a server reference exists. */
    public fun onServerStarting() {
        kernel.launch {
            kernel.onServerThread {
                val server = kernel.requireServer()
                known.clear()
                server.playerList.players.forEach { player ->
                    known.record(
                        KnownPlayerIdentity(
                            player.uuid,
                            player.gameProfile.name
                        )
                    )
                }
                bans.listedAccountIdentities().forEach {
                    known.record(KnownPlayerIdentity(it.id, it.name))
                }
            }

            mutes.load()

            kernel.onServerThread {
                mutes.activeMutes().forEach { mute ->
                    known.record(
                        KnownPlayerIdentity(
                            mute.playerId,
                            mute.playerName
                        )
                    )
                }
            }
        }
    }

    public fun onServerStopping() {
        mutes.shutdown()
        vanish.clearAll()
    }

    /** Records every joined player so offline moderation can resolve them later. */
    public fun onPlayerJoined(player: ServerPlayer) {
        known.record(
            KnownPlayerIdentity(
                player.uuid,
                player.gameProfile.name
            )
        )
    }

    public fun onPlayerQuit(playerId: UUID) {
        spies.clear(playerId)
        vanish.clear(playerId)
    }

    /** Server-thread confined mute gate used by communication and the chat gate. */
    public fun isMuted(playerId: UUID): Boolean =
        mutes.isMuted(playerId)

    /**
     * Server-thread confined visibility check used by communication reachability. Vanished players
     * stay visible to other vanished players.
     */
    public fun canBeSeenBy(viewerId: UUID, targetId: UUID): Boolean =
        !vanish.isVanished(targetId) || vanish.isVanished(viewerId)

    /**
     * Called by the composition root after a private message is delivered. Given as primitives so
     * administration never imports communication types. Must be called on the server thread. The
     * spy copy is rendered once per recipient so each moderator sees their own language.
     */
    public fun observePrivateMessage(
        senderId: UUID,
        senderName: String,
        targetId: UUID,
        targetName: String,
        text: String,
    ) {
        val recipients = spies.recipients(setOf(senderId, targetId))
        if (recipients.isEmpty()) return

        val copy = Notice(
            LocalizedText(
                MessageKey("administration.socialspy.copy"),
                listOf(
                    TextArgument.PlayerName(senderName),
                    TextArgument.PlayerName(targetName),
                    TextArgument.Literal(text),
                ),
            ),
            NoticeTone.INFO,
        )
        kernel.launch {
            recipients.forEach { recipientId ->
                val target = CommandFeedbackTarget.Player(recipientId)
                presentation.renderFor(recipientId, copy).forEach { line ->
                    kernel.message(target, adventure, line)
                }
            }
        }
    }

}

public fun createAdministrationFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    permissions: PermissionService,
    settings: () -> AdministrationSettings,
    known: MinecraftKnownPlayerResolver,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
): AdministrationFeature {
    val clock = Clock.systemUTC()
    val accountResolver = MinecraftAccountResolver(known)
    val protection = TargetProtectionPolicy(kernel) { settings().moderation }
    val audit = ModerationAuditService(
        repository = JdbcModerationAuditRepository(database, namespace),
        settings = { settings().moderation },
        clock = clock,
    )
    val notifier = ModerationNotifier(kernel, permissions, adventure, presentation)
    val kick = KickService(
        kernel = kernel,
        accountResolver = accountResolver,
        protection = protection,
        audit = audit,
    )
    val bans = BanService(
        kernel = kernel,
        backend = MinecraftBanBackend(kernel),
        accountResolver = accountResolver,
        protection = protection,
        audit = audit,
        settings = { settings().moderation },
        clock = clock,
    )
    val mutes = MuteService(
        kernel = kernel,
        repository = JdbcMuteRepository(database, namespace),
        accountResolver = accountResolver,
        protection = protection,
        audit = audit,
        settings = { settings().moderation },
        clock = clock,
    )
    val control = PlayerControlService(
        kernel = kernel,
        backend = MinecraftPlayerControlBackend(kernel),
        accountResolver = accountResolver,
        protection = protection,
        audit = audit,
    )
    val spies = SocialSpyService(kernel, audit)
    val vanish = VanishService(kernel, audit)

    return AdministrationFeature(
        kick = kick,
        bans = bans,
        mutes = mutes,
        control = control,
        spies = spies,
        vanish = vanish,
        known = known,
        notifier = notifier,
        settings = settings,
        kernel = kernel,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
    )
}
