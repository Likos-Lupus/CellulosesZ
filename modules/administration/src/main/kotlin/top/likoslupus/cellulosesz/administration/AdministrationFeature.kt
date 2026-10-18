package top.likoslupus.cellulosesz.administration

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.entitytools.EntityToolsService
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.administration.format.mutedChat
import top.likoslupus.cellulosesz.administration.integration.*
import top.likoslupus.cellulosesz.administration.jail.ConfinementQuery
import top.likoslupus.cellulosesz.administration.jail.JailSentenceMode
import top.likoslupus.cellulosesz.administration.jail.JailService
import top.likoslupus.cellulosesz.administration.login.LoginProtection
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.ModerationReason
import top.likoslupus.cellulosesz.administration.moderation.ModerationState
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.ban.BanService
import top.likoslupus.cellulosesz.administration.moderation.kick.KickService
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteService
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.administration.operator.PlayerControlService
import top.likoslupus.cellulosesz.administration.playerstate.PlayerAdjustmentService
import top.likoslupus.cellulosesz.administration.playerstate.PlayerStateService
import top.likoslupus.cellulosesz.administration.sign.SignEditService
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyService
import top.likoslupus.cellulosesz.administration.vanish.VanishService
import top.likoslupus.cellulosesz.administration.worldtools.PersonalViews
import top.likoslupus.cellulosesz.administration.worldtools.WorldToolsService
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.foundation.time.TemporaryDuration
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Public surface of the administration bounded context. Internals stay `internal`.
 *
 * The facade keeps the lifecycle-independent queries and the public-API integration port; server
 * lifecycle, observation hooks, and command assembly live in [AdministrationLifecycle],
 * [AdministrationObservation], and [AdministrationCommandFactory].
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
    private val playerState: PlayerStateService,
    private val entityTools: EntityToolsService,
    private val worldTools: WorldToolsService,
    private val personalViews: PersonalViews,
    private val signEdits: SignEditService,
    private val login: LoginProtection,
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

    private val lifecycle = AdministrationLifecycle(
        kernel = kernel,
        settings = settings,
        known = known,
        bans = bans,
        mutes = mutes,
        jail = jail,
        vanish = vanish,
        spies = spies,
        adjustments = adjustments,
        playerState = playerState,
        personalViews = personalViews,
        worldTools = worldTools,
        signEdits = signEdits,
        login = login,
        permissions = permissions,
    )

    private val observation = AdministrationObservation(
        kernel = kernel,
        settings = settings,
        spies = spies,
        vanish = vanish,
        known = known,
        audit = audit,
        adventure = adventure,
        presentation = presentation,
    )

    private val commandFactory = AdministrationCommandFactory(
        kick = kick,
        bans = bans,
        mutes = mutes,
        control = control,
        spies = spies,
        vanish = vanish,
        jail = jail,
        adjustments = adjustments,
        playerState = playerState,
        entityTools = entityTools,
        worldTools = worldTools,
        personalViews = personalViews,
        signEdits = signEdits,
        known = known,
        notifier = notifier,
        settings = settings,
        kernel = kernel,
        permissions = permissions,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
    )

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
                val sentence = jail.sentence(subjectId)
                val confinedRemaining = sentence?.let { term ->
                    when (term.mode) {
                        JailSentenceMode.ONLINE ->
                            term.remainingSeconds?.let(Duration::ofSeconds)

                        JailSentenceMode.WALL_CLOCK ->
                            term.expiresAt
                                    ?.let { Duration.between(Instant.now(), it) }
                                    ?.takeIf { !it.isNegative }

                        JailSentenceMode.PERMANENT -> null
                    }
                }
                val knownTarget = known.onlineById(subjectId) != null ||
                        known.knownById(subjectId) != null

                when (mute) {
                    null if !confined && !knownTarget -> ModerationIntegrationStatus.NotFound
                    else -> ModerationIntegrationStatus.Known(
                        ModerationStatusSummary(
                            muted = mute != null,
                            mutedUntil = mute?.expiresAt,
                            confined = confined,
                            confinedRemaining = confinedRemaining,
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
                ) ->
                    ModerationIntegrationOutcome.NotAuthorized

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

    public fun commands(): List<CommandDefinition> =
        commandFactory.create()

    /** Localized notice shown to a muted player whose chat is blocked. */
    public fun mutedChatNotice(): Notice =
        AdministrationMessages.mutedChat()

    /**
     * Re-publishes the jail command-restriction snapshot from config. The composition root calls
     * this after a reload; [onServerStarting] also seeds it, so it is never stale for a running
     * server. Must be called on the server thread.
     */
    public fun applyJailCommandRestrictions() {
        lifecycle.applyJailCommandRestrictions()
    }

    /** Seeds known identities and loads durable mutes. Must run when a server reference exists. */
    public fun onServerStarting() {
        lifecycle.onServerStarting()
    }

    public fun onServerStopping() {
        lifecycle.onServerStopping()
    }

    /** Records every joined player so offline moderation can resolve them later. */
    public fun onPlayerJoined(player: ServerPlayer) {
        lifecycle.onPlayerJoined(player)
    }

    public fun onPlayerQuit(playerId: UUID) {
        lifecycle.onPlayerQuit(playerId)
    }

    /** Server-thread confined mute gate used by communication and the chat gate. */
    public fun isMuted(playerId: UUID): Boolean =
        mutes.isMuted(playerId)

    /**
     * Server-thread login PvP gate: true when [attackerId] joined less than
     * `[administration.moderation].loginAttackDelaySeconds` ago and does not hold
     * `cellulosesz.pvpdelay.exempt`. The application damage gate calls this for the attacker.
     */
    public fun isLoginProtected(attackerId: UUID, now: Instant): Boolean {
        val delay = settings().moderation.loginAttackDelaySeconds
        if (!login.isProtected(attackerId, now, delay)) {
            return false
        }
        val player = kernel.requireServer().playerList.getPlayer(attackerId)
            ?: return false
        return !player.createCommandSourceStack().requiresPermission(
            permissions,
            CommandPermissions.PVP_DELAY_EXEMPT,
        )
    }

    /** Server-thread confined vanish query for join/quit announcement suppression. */
    public fun isVanished(playerId: UUID): Boolean =
        vanish.isVanished(playerId)

    /** Command roots blocked while muted, from live config. Server thread. */
    public fun mutedCommandRoots(): List<String> =
        settings().moderation.mutedCommands

    /** Social-spy command observation; the root command only, never its arguments. Server thread. */
    public fun observeCommand(actorId: UUID?, command: String) {
        observation.observeCommand(actorId, command)
    }

    /** Forces vanish on for a silent join. Server-thread confined. */
    public fun forceVanish(playerId: UUID) {
        observation.forceVanish(playerId)
    }

    /** True when a vanished player's join/quit announcement must be hidden. Server thread. */
    public fun shouldHideJoinQuit(playerId: UUID): Boolean =
        settings().vanish.hideJoinQuit && vanish.isVanished(playerId)

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
        lifecycle.onPlayerTick(player)
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
        observation.recordInventoryClear(
            actorId,
            targetId,
            targetName,
            cleared
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
        observation.observePrivateMessage(
            senderId,
            senderName,
            targetId,
            targetName,
            text
        )
    }

}

private const val DEFAULT_JAIL_REASON: String = "Jailed by an administrator"
