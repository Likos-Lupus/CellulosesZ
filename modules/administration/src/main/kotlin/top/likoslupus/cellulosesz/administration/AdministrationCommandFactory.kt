package top.likoslupus.cellulosesz.administration

import top.likoslupus.cellulosesz.administration.amusement.FunCommands
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.entitytools.EntityToolsCommands
import top.likoslupus.cellulosesz.administration.entitytools.EntityToolsService
import top.likoslupus.cellulosesz.administration.jail.JailCommands
import top.likoslupus.cellulosesz.administration.jail.JailService
import top.likoslupus.cellulosesz.administration.moderation.ban.BanCommands
import top.likoslupus.cellulosesz.administration.moderation.ban.BanService
import top.likoslupus.cellulosesz.administration.moderation.ban.IpBanCommands
import top.likoslupus.cellulosesz.administration.moderation.kick.KickCommands
import top.likoslupus.cellulosesz.administration.moderation.kick.KickService
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteCommands
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteService
import top.likoslupus.cellulosesz.administration.moderation.notify.ModerationNotifier
import top.likoslupus.cellulosesz.administration.operator.PlayerControlCommands
import top.likoslupus.cellulosesz.administration.operator.PlayerControlService
import top.likoslupus.cellulosesz.administration.playerstate.ExperienceCommands
import top.likoslupus.cellulosesz.administration.playerstate.PlayerAdjustmentService
import top.likoslupus.cellulosesz.administration.playerstate.PlayerStateCommands
import top.likoslupus.cellulosesz.administration.playerstate.PlayerStateService
import top.likoslupus.cellulosesz.administration.playerstate.SpeedCommands
import top.likoslupus.cellulosesz.administration.sign.SignCommands
import top.likoslupus.cellulosesz.administration.sign.SignEditService
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyCommands
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyService
import top.likoslupus.cellulosesz.administration.vanish.VanishCommands
import top.likoslupus.cellulosesz.administration.vanish.VanishService
import top.likoslupus.cellulosesz.administration.worldtools.PersonalViews
import top.likoslupus.cellulosesz.administration.worldtools.WorldToolsCommands
import top.likoslupus.cellulosesz.administration.worldtools.WorldToolsService
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** Assembles every administration command definition from the feature's services. */
internal class AdministrationCommandFactory(
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
    private val known: MinecraftKnownPlayerResolver,
    private val notifier: ModerationNotifier,
    private val settings: () -> AdministrationSettings,
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
) {

    fun create(): List<CommandDefinition> {
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
            permissions,
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
            playerState,
            permissions,
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
        ) + EntityToolsCommands.commands(
            entityTools,
            { settings().entityTools },
            permissions,
            adventure,
            presentation
        ) + WorldToolsCommands.commands(
            worldTools,
            personalViews,
            { settings().worldTools },
            permissions,
            adventure,
            presentation
        ) + FunCommands.commands(
            { settings().amusement },
            kernel,
            adventure,
            presentation
        ) + SignCommands.commands(
            signEdits,
            permissions,
            { settings().signEditing },
            adventure,
            presentation
        )
    }
}
