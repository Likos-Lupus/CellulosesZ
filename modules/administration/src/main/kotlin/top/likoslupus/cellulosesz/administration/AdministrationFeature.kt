package top.likoslupus.cellulosesz.administration

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.administration.integration.*
import top.likoslupus.cellulosesz.administration.jail.*
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.ModerationState
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.administration.moderation.audit.JdbcModerationAuditRepository
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditAction
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.ban.BanCommands
import top.likoslupus.cellulosesz.administration.moderation.ban.BanService
import top.likoslupus.cellulosesz.administration.moderation.ban.IpBanCommands
import top.likoslupus.cellulosesz.administration.moderation.ban.MinecraftBanBackend
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.kick.KickCommands
import top.likoslupus.cellulosesz.administration.moderation.kick.KickService
import top.likoslupus.cellulosesz.administration.moderation.mute.*
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.administration.operator.MinecraftPlayerControlBackend
import top.likoslupus.cellulosesz.administration.operator.PlayerControlCommands
import top.likoslupus.cellulosesz.administration.operator.PlayerControlService
import top.likoslupus.cellulosesz.administration.playerstate.*
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyCommands
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyService
import top.likoslupus.cellulosesz.administration.vanish.VanishCommands
import top.likoslupus.cellulosesz.administration.vanish.VanishService
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.WorldPosition
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
import top.likoslupus.cellulosesz.foundation.time.TemporaryDuration
import java.time.Clock
import java.time.Duration
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
    private val jail: JailService,
    private val adjustments: PlayerAdjustmentService,
    private val known: MinecraftKnownPlayerResolver,
    private val notifier: ModerationNotifier,
    private val audit: ModerationAuditService,
    private val settings: () -> AdministrationSettings,
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
    private val events: ModerationEventBus,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
) : ConfinementQuery {

    /** Narrow integration surface for the public API; never exposes the internal services. */
    public val integration: ModerationIntegrationPort = ModerationIntegration()

    private inner class ModerationIntegration : ModerationIntegrationPort {

        override fun isMuted(viewerId: UUID, subjectId: UUID): Boolean =
            mutes.isMuted(subjectId)

        override fun observe(observer: ModerationDomainObserver): ModerationEventRegistration =
            events.subscribe(observer)

        override suspend fun status(
            viewerId: UUID,
            subjectId: UUID,
        ): ModerationIntegrationStatus {
            if (!authorizeViewer(viewerId, CommandPermissions.MUTE_INFO)) {
                return ModerationIntegrationStatus.NotAuthorized
            }
            val muteState = mutes.state()
            val jailState = jail.state()
            if (muteState == ModerationState.STOPPING
                || jailState == ModerationState.STOPPING
            ) {
                return ModerationIntegrationStatus.RuntimeStopping
            }
            if (muteState == ModerationState.LOADING
                || jailState == ModerationState.LOADING
            ) {
                return ModerationIntegrationStatus.StorageUnavailable
            }
            return kernel.onServerThread {
                val mute = mutes.activeMute(subjectId)
                val confined = jail.isConfined(subjectId)
                val knownTarget = known.onlineById(subjectId) != null ||
                        known.knownById(subjectId) != null

                when (mute) {
                    null if !confined && !knownTarget -> ModerationIntegrationStatus.NotFound
                    else -> ModerationIntegrationStatus.Known(
                        ModerationStatusSummary(
                            muted = mute != null,
                            mutedUntil = mute?.expiresAt,
                            confined = confined,
                        )
                    )
                }
            }
        }

        override suspend fun mute(
            actor: ModerationIntegrationActor,
            targetId: UUID,
            reason: String,
            duration: Duration?,
        ): ModerationIntegrationOutcome =
            when {
                !authorize(
                    actor,
                    when (duration) {
                        null -> CommandPermissions.MUTE
                        else -> CommandPermissions.TEMP_MUTE
                    }
                ) -> ModerationIntegrationOutcome.NotAuthorized

                duration != null && (duration.isZero || duration.isNegative) ->
                    ModerationIntegrationOutcome.InvalidDuration

                else -> {
                    val targetName = resolveName(targetId)
                        ?: return ModerationIntegrationOutcome.TargetUnknown
                    val parsed = parseReason(
                        reason,
                        settings().moderation.defaultMuteReason
                    ) ?: return ModerationIntegrationOutcome.InvalidReason
                    val temporary = duration?.let(::TemporaryDuration)
                    return mutes.mute(
                        actor.toDomain(),
                        targetName,
                        parsed,
                        temporary
                    ).toIntegration()
                }
            }

        override suspend fun unmute(
            actor: ModerationIntegrationActor,
            targetId: UUID,
        ): ModerationIntegrationOutcome {
            if (!authorize(actor, CommandPermissions.UNMUTE)) {
                return ModerationIntegrationOutcome.NotAuthorized
            }

            val targetName = resolveName(targetId)
                ?: return ModerationIntegrationOutcome.TargetUnknown
            return mutes.unmute(actor.toDomain(), targetName).toIntegration()
        }

        override suspend fun confine(
            actor: ModerationIntegrationActor,
            targetId: UUID,
            reason: String,
        ): ModerationIntegrationOutcome {
            if (!authorize(actor, CommandPermissions.JAIL)) {
                return ModerationIntegrationOutcome.NotAuthorized
            }

            val targetName = resolveName(targetId)
                ?: return ModerationIntegrationOutcome.TargetUnknown
            val parsed = parseReason(reason, DEFAULT_JAIL_REASON)
                ?: return ModerationIntegrationOutcome.InvalidReason
            val jailName = kernel.onServerThread { defaultJailName() }
                ?: return ModerationIntegrationOutcome.Unavailable
            return jail.jail(
                actor.toDomain(),
                targetName,
                jailName,
                null,
                parsed
            ).toIntegration()
        }

        override suspend fun release(
            actor: ModerationIntegrationActor,
            targetId: UUID,
        ): ModerationIntegrationOutcome {
            if (!authorize(actor, CommandPermissions.UNJAIL)) {
                return ModerationIntegrationOutcome.NotAuthorized
            }

            val targetName = resolveName(targetId)
                ?: return ModerationIntegrationOutcome.TargetUnknown
            return jail.release(actor.toDomain(), targetName).toIntegration()
        }

    }

    /**
     * Authorizes an action actor. A player must be online and hold [spec]; an addon actor is only
     * ever constructed by the application adapter after it verified the addon's capability, so it is
     * trusted here (it still gets its own audit actor type).
     */
    private suspend fun authorize(
        actor: ModerationIntegrationActor,
        spec: PermissionSpec,
    ): Boolean =
        when (actor) {
            is ModerationIntegrationActor.Addon -> true
            is ModerationIntegrationActor.Player -> authorizeViewer(actor.playerId, spec)
        }

    private suspend fun authorizeViewer(viewerId: UUID, spec: PermissionSpec): Boolean =
        kernel.onServerThread {
            kernel.requireServer().playerList
                    .getPlayer(viewerId)
                    ?.createCommandSourceStack()
                    ?.requiresPermission(permissions, spec)
                ?: false
        }

    private suspend fun ModerationIntegrationActor.toDomain(): ModerationActor =
        when (this) {
            is ModerationIntegrationActor.Addon -> ModerationActor.Addon(addonId)
            is ModerationIntegrationActor.Player -> ModerationActor.Player(
                playerId,
                kernel.onServerThread {
                    kernel.requireServer().playerList.getPlayer(playerId)?.gameProfile?.name
                } ?: playerId.toString(),
            )
        }

    private suspend fun resolveName(targetId: UUID): String? =
        kernel.onServerThread {
            known.onlineById(targetId)?.name
                ?: known.knownById(targetId)?.name
        }

    private fun parseReason(raw: String, default: String): ModerationReason? {
        val effective = raw.trim().ifEmpty { default }
        return ModerationReason.parse(
            effective,
            settings().moderation.maxReasonLength
        )
    }

    private fun defaultJailName(): String? =
        settings().jail.defaultJail.let {
            when {
                it.isNotBlank() -> it
                else -> jail.locations().firstOrNull()?.name?.value
            }
        }

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
        ) + ExperienceCommands.commands(
            adjustments,
            known,
            adventure,
            presentation
        ) + SpeedCommands.commands(
            adjustments,
            known,
            adventure,
            presentation
        ) + JailCommands.commands(
            jail,
            notifier,
            { settings().moderation },
            kernel,
            messages,
            adventure,
            presentation
        )
    }

    /** Localized notice shown to a muted player whose chat is blocked. */
    public fun mutedChatNotice(): Notice =
        AdministrationMessages.mutedChat()

    /**
     * Re-publishes the jail command-restriction snapshot from config. The composition root calls
     * this after a reload; [onServerStarting] also seeds it, so it is never stale for a running
     * server. Must be called on the server thread.
     */
    public fun applyJailCommandRestrictions() {
        RestrictCommandsState.set(JailCommandAccessPolicy.snapshotFrom(settings().jail))
    }

    /** Seeds known identities and loads durable mutes. Must run when a server reference exists. */
    public fun onServerStarting() {
        RestrictCommandsState.set(JailCommandAccessPolicy.snapshotFrom(settings().jail))
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
            jail.load()

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
        jail.shutdown()
        vanish.clearAll()
        adjustments.restoreAllSpeeds()
    }

    /** Records every joined player so offline moderation can resolve them later. */
    public fun onPlayerJoined(player: ServerPlayer) {
        known.record(
            KnownPlayerIdentity(
                player.uuid,
                player.gameProfile.name
            )
        )
        jail.onPlayerJoined(player.uuid)
    }

    public fun onPlayerQuit(playerId: UUID) {
        spies.clear(playerId)
        vanish.clear(playerId)
        adjustments.restoreSpeed(playerId)
    }

    /** Server-thread confined mute gate used by communication and the chat gate. */
    public fun isMuted(playerId: UUID): Boolean =
        mutes.isMuted(playerId)

    /** Server-thread confined confinement query consumed by movement's teleport path. */
    override fun isConfined(playerId: UUID): Boolean =
        jail.isConfined(playerId)

    /**
     * Server-thread confinement tick. Re-anchors jailed players that left the allowed radius and
     * drives release-pending players out. Throttled to `confinementCheckIntervalTicks`, except for a
     * player that just respawned, who is re-checked on the next tick. It never consults
     * `[administration.jail].enabled`, so existing sentences keep being enforced when the capability
     * is disabled.
     */
    public fun onPlayerTick(player: ServerPlayer) {
        val config = settings().jail
        val server = kernel.requireServer()
        val respawned = JailRespawnState.consume(player.uuid)
        val interval = config.confinementCheckIntervalTicks
        if (!respawned && (interval <= 0 || server.tickCount % interval != 0)) {
            return
        }

        jail.onObservation(
            playerId = player.uuid,
            dimension = player.level().dimension().identifier().toString(),
            x = player.x,
            y = player.y,
            z = player.z,
        )
    }

    /**
     * Server-thread confined visibility check used by communication reachability. Vanished players
     * stay visible to other vanished players.
     */
    public fun canBeSeenBy(viewerId: UUID, targetId: UUID): Boolean =
        !vanish.isVanished(targetId) || vanish.isVanished(viewerId)

    /**
     * Best-effort audit for an administrative utility action that mutates another player (for
     * example clearing their inventory). Given as primitives so utility never imports
     * administration. The game action already happened, so a failed write is only logged.
     */
    public suspend fun recordInventoryClear(
        actorId: UUID,
        targetId: UUID,
        targetName: String,
        cleared: Boolean,
    ): Boolean =
        audit.record(
            actor = ModerationActor.Player(
                id = actorId,
                name = known.onlineById(actorId)?.name ?: actorId.toString(),
            ),
            action = ModerationAuditAction.CLEAR_INVENTORY,
            target = PlayerIdentity(targetId, targetName),
            reason = if (cleared) "cleared" else "failed",
        )

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

private const val DEFAULT_JAIL_REASON: String = "Jailed by an administrator"

/** Maps the internal mute outcome onto the integration port's result set. */
private fun MuteResult.toIntegration(): ModerationIntegrationOutcome =
    when (this) {
        is MuteResult.Applied ->
            when {
                auditRecorded -> ModerationIntegrationOutcome.Applied(actionId)
                else -> ModerationIntegrationOutcome.PartiallyApplied
            }

        MuteResult.AlreadyMuted -> ModerationIntegrationOutcome.AlreadyInState
        MuteResult.SelfTarget -> ModerationIntegrationOutcome.ProtectedTarget
        MuteResult.TargetProtected -> ModerationIntegrationOutcome.ProtectedTarget
        MuteResult.TargetUnknown -> ModerationIntegrationOutcome.TargetUnknown
        MuteResult.DurationTooLong -> ModerationIntegrationOutcome.InvalidDuration
        MuteResult.StorageUnavailable -> ModerationIntegrationOutcome.StorageUnavailable
        MuteResult.RuntimeStopping -> ModerationIntegrationOutcome.RuntimeStopping
    }

/** Maps the internal unmute outcome onto the integration port's result set. */
private fun UnmuteResult.toIntegration(): ModerationIntegrationOutcome =
    when (this) {
        is UnmuteResult.Unmuted ->
            when {
                auditRecorded -> ModerationIntegrationOutcome.Applied(actionId)
                else -> ModerationIntegrationOutcome.PartiallyApplied
            }

        UnmuteResult.TargetUnknown -> ModerationIntegrationOutcome.TargetUnknown
        UnmuteResult.NotMuted -> ModerationIntegrationOutcome.AlreadyInState
        UnmuteResult.StorageUnavailable -> ModerationIntegrationOutcome.StorageUnavailable
        UnmuteResult.RuntimeStopping -> ModerationIntegrationOutcome.RuntimeStopping
    }

/** Maps the internal jail outcome onto the integration port's result set. */
private fun JailActionOutcome.toIntegration(): ModerationIntegrationOutcome =
    when (this) {
        is JailActionOutcome.Jailed ->
            when {
                auditRecorded -> ModerationIntegrationOutcome.Applied(actionId)
                else -> ModerationIntegrationOutcome.PartiallyApplied
            }

        is JailActionOutcome.ReleaseRequested ->
            actionId.let {
                when {
                    it == null -> ModerationIntegrationOutcome.AlreadyInState
                    !auditRecorded -> ModerationIntegrationOutcome.PartiallyApplied
                    else -> ModerationIntegrationOutcome.Applied(it)
                }
            }

        JailActionOutcome.AlreadyJailed -> ModerationIntegrationOutcome.AlreadyInState
        JailActionOutcome.NotJailed -> ModerationIntegrationOutcome.AlreadyInState
        JailActionOutcome.TargetUnknown -> ModerationIntegrationOutcome.TargetUnknown
        JailActionOutcome.SelfTarget -> ModerationIntegrationOutcome.ProtectedTarget
        JailActionOutcome.TargetProtected -> ModerationIntegrationOutcome.ProtectedTarget
        JailActionOutcome.InvalidJailName -> ModerationIntegrationOutcome.Unavailable
        is JailActionOutcome.JailUnknown -> ModerationIntegrationOutcome.Unavailable
        JailActionOutcome.NoDefaultJail -> ModerationIntegrationOutcome.Unavailable
        JailActionOutcome.Disabled -> ModerationIntegrationOutcome.Unavailable
        JailActionOutcome.StorageUnavailable -> ModerationIntegrationOutcome.StorageUnavailable
        JailActionOutcome.RuntimeStopping -> ModerationIntegrationOutcome.RuntimeStopping
    }

public fun createAdministrationFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    permissions: PermissionService,
    settings: () -> AdministrationSettings,
    known: MinecraftKnownPlayerResolver,
    jailTransfer: () -> JailTransferPort,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
): AdministrationFeature {
    val clock = Clock.systemUTC()
    val accountResolver = MinecraftAccountResolver(known)
    val protection = TargetProtectionPolicy(kernel) { settings().moderation }
    val events = ModerationEventBus()
    val audit = ModerationAuditService(
        repository = JdbcModerationAuditRepository(database, namespace),
        settings = { settings().moderation },
        clock = clock,
    )
    val notifier = ModerationNotifier(
        kernel,
        permissions,
        adventure,
        presentation
    )
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
        events = events::publish,
    )
    val control = PlayerControlService(
        kernel = kernel,
        backend = MinecraftPlayerControlBackend(kernel),
        accountResolver = accountResolver,
        protection = protection,
        audit = audit,
    )
    val spies = SocialSpyService(kernel, audit)
    val vanish = VanishService(kernel, audit, events = events::publish)
    val adjustments = PlayerAdjustmentService(
        backend = MinecraftPlayerAdjustmentBackend(kernel),
        known = known,
        settings = { settings().playerAdjustments },
    )
    val jail = JailService(
        runtime = KernelJailRuntime(kernel),
        locations = JdbcJailLocationRepository(database, namespace),
        sentences = JdbcJailSentenceRepository(database, namespace),
        accountResolver = accountResolver,
        protection = protection,
        audit = audit,
        transfer = jailTransfer,
        positions = { playerId ->
            kernel.requireServer().playerList.getPlayer(playerId)?.let { player ->
                WorldPosition(
                    dimension = player.level().dimension().identifier().toString(),
                    x = player.x,
                    y = player.y,
                    z = player.z,
                    yaw = player.yRot,
                    pitch = player.xRot,
                )
            }
        },
        settings = { settings().jail },
        clock = clock,
        events = events::publish,
    )

    return AdministrationFeature(
        kick = kick,
        bans = bans,
        mutes = mutes,
        control = control,
        spies = spies,
        vanish = vanish,
        jail = jail,
        adjustments = adjustments,
        known = known,
        notifier = notifier,
        audit = audit,
        settings = settings,
        kernel = kernel,
        permissions = permissions,
        events = events,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
    )
}
