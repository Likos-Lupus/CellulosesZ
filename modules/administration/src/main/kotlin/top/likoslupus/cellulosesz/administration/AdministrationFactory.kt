package top.likoslupus.cellulosesz.administration

import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.entitytools.EntityToolsService
import top.likoslupus.cellulosesz.administration.integration.ModerationEventBus
import top.likoslupus.cellulosesz.administration.jail.JailService
import top.likoslupus.cellulosesz.administration.jail.JailTransferPort
import top.likoslupus.cellulosesz.administration.jail.JdbcJailLocationRepository
import top.likoslupus.cellulosesz.administration.jail.JdbcJailSentenceRepository
import top.likoslupus.cellulosesz.administration.jail.KernelJailRuntime
import top.likoslupus.cellulosesz.administration.login.LoginProtection
import top.likoslupus.cellulosesz.administration.moderation.audit.JdbcModerationAuditRepository
import top.likoslupus.cellulosesz.administration.moderation.audit.ModerationAuditService
import top.likoslupus.cellulosesz.administration.moderation.ban.BanService
import top.likoslupus.cellulosesz.administration.moderation.ban.MinecraftBanBackend
import top.likoslupus.cellulosesz.administration.moderation.identity.MinecraftAccountResolver
import top.likoslupus.cellulosesz.administration.moderation.kick.KickService
import top.likoslupus.cellulosesz.administration.moderation.mute.JdbcMuteRepository
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteService
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.administration.moderation.protection.TargetProtectionPolicy
import top.likoslupus.cellulosesz.administration.operator.MinecraftPlayerControlBackend
import top.likoslupus.cellulosesz.administration.operator.PlayerControlService
import top.likoslupus.cellulosesz.administration.playerstate.MinecraftPlayerAdjustmentBackend
import top.likoslupus.cellulosesz.administration.playerstate.MinecraftPlayerStateBackend
import top.likoslupus.cellulosesz.administration.playerstate.PlayerAdjustmentService
import top.likoslupus.cellulosesz.administration.playerstate.PlayerStateService
import top.likoslupus.cellulosesz.administration.sign.SignEditService
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyService
import top.likoslupus.cellulosesz.administration.vanish.VanishService
import top.likoslupus.cellulosesz.administration.worldtools.PersonalViews
import top.likoslupus.cellulosesz.administration.worldtools.WorldToolsService
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.WorldPosition
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.time.Clock

/** Composition-root entry point for the administration bounded context. */
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
    val vanish = VanishService(
        kernel,
        audit,
        { settings().vanish },
        events = events::publish,
    )
    val adjustments = PlayerAdjustmentService(
        backend = MinecraftPlayerAdjustmentBackend(kernel),
        known = known,
        settings = { settings().playerAdjustments },
    )
    val playerState = PlayerStateService(
        backend = MinecraftPlayerStateBackend(kernel),
        known = known,
        settings = { settings().playerAdjustments },
        clock = clock,
    )
    val entityTools = EntityToolsService()
    val worldTools = WorldToolsService()
    val personalViews = PersonalViews()
    val signEdits = SignEditService()
    val login = LoginProtection()
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
        playerState = playerState,
        entityTools = entityTools,
        worldTools = worldTools,
        personalViews = personalViews,
        signEdits = signEdits,
        login = login,
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
