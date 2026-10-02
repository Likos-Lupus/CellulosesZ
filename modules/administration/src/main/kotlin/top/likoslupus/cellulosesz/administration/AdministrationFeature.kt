package top.likoslupus.cellulosesz.administration

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.FileModerationAuditRepository
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.ban.BanCommands
import top.likoslupus.cellulosesz.administration.moderation.ban.BanService
import top.likoslupus.cellulosesz.administration.moderation.ban.IpBanCommands
import top.likoslupus.cellulosesz.administration.moderation.ban.MinecraftBanBackend
import top.likoslupus.cellulosesz.administration.moderation.identity.KnownPlayerIndex
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.kick.KickCommands
import top.likoslupus.cellulosesz.administration.moderation.kick.KickService
import top.likoslupus.cellulosesz.administration.moderation.mute.FileMuteRepository
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
import top.likoslupus.cellulosesz.core.command.messagePlayer
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.Messages
import java.nio.file.Path
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
    private val known: KnownPlayerIndex,
    private val notifier: ModerationNotifier,
    private val settings: () -> AdministrationSettings,
    private val kernel: RuntimeKernel,
) {

    public fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val moderation = { settings().moderation }
        KickCommands.register(dispatcher, kick, notifier, moderation, kernel)
        BanCommands.register(dispatcher, bans, notifier, moderation, kernel)
        IpBanCommands.register(dispatcher, bans, notifier, moderation, kernel)
        MuteCommands.register(dispatcher, mutes, notifier, moderation, kernel)
        PlayerControlCommands.register(dispatcher, control, kernel)
        SocialSpyCommands.register(dispatcher, spies, kernel)
        VanishCommands.register(dispatcher, vanish, kernel)
        PlayerStateCommands.register(dispatcher)
    }

    /** Seeds known identities and loads durable mutes. Must run when a server reference exists. */
    public fun onServerStarting() {
        kernel.launch {
            kernel.onServerThread {
                val server = kernel.requireServer()
                known.clear()
                server.playerList.players.forEach { player ->
                    known.record(
                        PlayerIdentity(
                            player.uuid,
                            player.gameProfile.name
                        )
                    )
                }
                bans.listedAccountIdentities().forEach { known.record(it) }
            }

            mutes.load()

            kernel.onServerThread {
                mutes.activeMutes().forEach { mute ->
                    known.record(
                        PlayerIdentity(
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
            PlayerIdentity(
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
     * Called by the composition root after a private message is delivered. Given as primitives so
     * administration never imports communication types. Must be called on the server thread.
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

        val copy = Messages.raw("[spy] $senderName -> $targetName: $text")
        kernel.launch {
            recipients.forEach {
                kernel.messagePlayer(
                    it,
                    copy
                )
            }
        }
    }

}

public fun createAdministrationFeature(
    kernel: RuntimeKernel,
    dataRoot: () -> Path,
    settings: () -> AdministrationSettings,
): AdministrationFeature {
    val clock = Clock.systemUTC()
    val known = KnownPlayerIndex()
    val accountResolver = MinecraftAccountResolver(kernel, known)
    val protection = TargetProtectionPolicy(kernel) { settings().moderation }
    val audit = ModerationAuditService(
        repository = FileModerationAuditRepository(dataRoot),
        settings = { settings().moderation },
        clock = clock,
    )
    val notifier = ModerationNotifier(kernel)
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
        repository = FileMuteRepository(dataRoot),
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
    )
}
