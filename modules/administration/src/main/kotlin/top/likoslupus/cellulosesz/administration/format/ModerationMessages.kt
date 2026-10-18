package top.likoslupus.cellulosesz.administration.format

import top.likoslupus.cellulosesz.administration.moderation.ban.BanResult
import top.likoslupus.cellulosesz.administration.moderation.ban.IpBanResult
import top.likoslupus.cellulosesz.administration.moderation.ban.IpUnbanResult
import top.likoslupus.cellulosesz.administration.moderation.ban.UnbanResult
import top.likoslupus.cellulosesz.administration.moderation.kick.KickAllResult
import top.likoslupus.cellulosesz.administration.moderation.kick.KickResult
import top.likoslupus.cellulosesz.administration.moderation.mute.Mute
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteResult
import top.likoslupus.cellulosesz.administration.moderation.mute.UnmuteResult
import top.likoslupus.cellulosesz.administration.operator.PlayerControlResult
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyResult
import top.likoslupus.cellulosesz.administration.vanish.VanishResult
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import java.time.Instant

internal fun AdministrationMessages.kick(result: KickResult): Notice =
    when (result) {
        is KickResult.Kicked -> withAudit(
            success(
                MessageKey("administration.kick.success"),
                player(result.target.name)
            ),
            result.auditRecorded,
        )

        KickResult.TargetOffline -> error(MessageKey("administration.kick.target_offline"))
        KickResult.SelfTarget -> error(MessageKey("administration.kick.self"))
        KickResult.TargetProtected -> error(MessageKey("administration.kick.protected"))
    }

internal fun AdministrationMessages.kickAll(result: KickAllResult): List<Notice> =
    buildList {
        add(
            success(
                MessageKey("administration.kickall.success"),
                count(result.kicked)
            )
        )
        if (result.protected > 0) {
            add(
                warning(
                    MessageKey("administration.kickall.protected"),
                    count(result.protected)
                )
            )
        }
        if (result.skippedIssuer) {
            add(info(MessageKey("administration.kickall.issuer")))
        }
    }

internal fun AdministrationMessages.ban(result: BanResult, now: Instant): Notice =
    when (result) {
        is BanResult.Applied -> {
            val expiresAt = result.expiresAt
            withAudit(
                when (expiresAt) {
                    null -> success(
                        MessageKey("administration.ban.success"),
                        player(result.target.name)
                    )

                    else -> success(
                        MessageKey("administration.ban.success_temp"),
                        player(result.target.name),
                        elapsed(expiresAt, now),
                    )
                },
                result.auditRecorded,
            )
        }

        BanResult.TargetUnknown -> error(MessageKey("administration.ban.target_unknown"))
        BanResult.SelfTarget -> error(MessageKey("administration.ban.self"))
        BanResult.TargetProtected -> error(MessageKey("administration.ban.protected"))
        BanResult.AlreadyBanned -> error(MessageKey("administration.ban.already"))
        BanResult.DurationTooLong -> error(MessageKey("administration.ban.duration_too_long"))
        BanResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.unban(result: UnbanResult): Notice =
    when (result) {
        is UnbanResult.Unbanned -> withAudit(
            success(
                MessageKey("administration.unban.success"),
                player(result.target.name)
            ),
            result.auditRecorded,
        )

        UnbanResult.TargetUnknown -> error(MessageKey("administration.unban.target_unknown"))
        UnbanResult.NotBanned -> error(MessageKey("administration.unban.not_banned"))
        UnbanResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.ipBan(result: IpBanResult, now: Instant): Notice =
    when (result) {
        is IpBanResult.Applied -> {
            val expiresAt = result.expiresAt
            withAudit(
                when (expiresAt) {
                    null -> success(
                        MessageKey("administration.ipban.success"),
                        literal(result.address)
                    )

                    else -> success(
                        MessageKey("administration.ipban.success_temp"),
                        literal(result.address),
                        elapsed(expiresAt, now),
                    )
                },
                result.auditRecorded,
            )
        }

        IpBanResult.InvalidAddress -> error(MessageKey("administration.ipban.invalid"))
        IpBanResult.TargetProtected -> error(MessageKey("administration.ipban.protected"))
        IpBanResult.AlreadyBanned -> error(MessageKey("administration.ipban.already"))
        IpBanResult.DurationTooLong -> error(MessageKey("administration.ipban.duration_too_long"))
        IpBanResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.ipUnban(result: IpUnbanResult): Notice =
    when (result) {
        is IpUnbanResult.Unbanned -> withAudit(
            success(
                MessageKey("administration.ipunban.success"),
                literal(result.address)
            ),
            result.auditRecorded,
        )

        IpUnbanResult.InvalidAddress -> error(MessageKey("administration.ipunban.invalid"))
        IpUnbanResult.NotBanned -> error(MessageKey("administration.ipunban.not_banned"))
        IpUnbanResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.mute(result: MuteResult, now: Instant): Notice =
    when (result) {
        is MuteResult.Applied -> {
            val expiresAt = result.expiresAt
            withAudit(
                when (expiresAt) {
                    null ->
                        success(
                            MessageKey("administration.mute.success"),
                            player(result.target.name)
                        )

                    else ->
                        success(
                            MessageKey("administration.mute.success_temp"),
                            player(result.target.name),
                            elapsed(expiresAt, now),
                        )
                },
                result.auditRecorded,
            )
        }

        MuteResult.TargetUnknown -> error(MessageKey("administration.mute.target_unknown"))
        MuteResult.SelfTarget -> error(MessageKey("administration.mute.self"))
        MuteResult.TargetProtected -> error(MessageKey("administration.mute.protected"))
        MuteResult.AlreadyMuted -> error(MessageKey("administration.mute.already"))
        MuteResult.DurationTooLong -> error(MessageKey("administration.mute.duration_too_long"))
        MuteResult.StorageUnavailable -> error(MessageKey("administration.mute.storage"))
        MuteResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.unmute(result: UnmuteResult): Notice =
    when (result) {
        is UnmuteResult.Unmuted -> withAudit(
            success(
                MessageKey("administration.unmute.success"),
                player(result.target.name)
            ),
            result.auditRecorded,
        )

        UnmuteResult.TargetUnknown -> error(MessageKey("administration.unmute.target_unknown"))
        UnmuteResult.NotMuted -> error(MessageKey("administration.unmute.not_muted"))
        UnmuteResult.StorageUnavailable -> error(MessageKey("administration.unmute.storage"))
        UnmuteResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
    }

internal fun AdministrationMessages.mutedTargetNotice(reason: String): Notice =
    Notice(
        LocalizedText(
            MessageKey("administration.mute.target_notice"),
            listOf(literal(reason))
        ),
        NoticeTone.WARNING,
    )

internal fun AdministrationMessages.unmutedTargetNotice(): Notice =
    Notice(
        LocalizedText(MessageKey("administration.unmute.target_notice")),
        NoticeTone.INFO
    )

internal fun AdministrationMessages.muteInfo(mute: Mute, now: Instant): DetailView = DetailView(
    title = LocalizedText(MessageKey("administration.muteinfo.title")),
    fields = listOf(
        field(
            MessageKey("administration.muteinfo.target"),
            bare(player(mute.playerName))
        ),
        field(
            MessageKey("administration.muteinfo.state"),
            LocalizedText(MessageKey("administration.muteinfo.state_value"))
        ),
        field(
            MessageKey("administration.muteinfo.reason"),
            bare(literal(mute.reason))
        ),
        field(
            MessageKey("administration.muteinfo.actor"),
            bare(literal(mute.actor.name))
        ),
        field(
            MessageKey("administration.muteinfo.term_label"),
            remaining(mute.expiresAt, now)
        ),
    ),
)

internal fun AdministrationMessages.kill(result: PlayerControlResult): Notice =
    when (result) {
        is PlayerControlResult.Killed -> withAudit(
            success(
                MessageKey("administration.kill.success"),
                player(result.target.name)
            ),
            result.auditRecorded,
        )

        PlayerControlResult.TargetOffline -> error(MessageKey("administration.kill.target_offline"))
        PlayerControlResult.SelfTarget -> error(MessageKey("administration.kill.self"))
        PlayerControlResult.TargetProtected -> error(MessageKey("administration.kill.protected"))
        PlayerControlResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
        else -> error(MessageKey("administration.error.unexpected"))
    }

internal fun AdministrationMessages.gameMode(result: PlayerControlResult): Notice =
    when (result) {
        is PlayerControlResult.GameModeChanged -> withAudit(
            success(
                MessageKey("administration.gamemode.success"),
                player(result.target.name),
                literal(result.mode.name.lowercase()),
            ),
            result.auditRecorded,
        )

        PlayerControlResult.TargetOffline -> error(MessageKey("administration.gamemode.target_offline"))
        PlayerControlResult.TargetProtected -> error(MessageKey("administration.gamemode.protected"))
        PlayerControlResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
        else -> error(MessageKey("administration.error.unexpected"))
    }

internal fun AdministrationMessages.sudo(result: PlayerControlResult): Notice =
    when (result) {
        is PlayerControlResult.SudoExecuted -> withAudit(
            success(
                MessageKey("administration.sudo.success"),
                player(result.target.name)
            ),
            result.auditRecorded,
        )

        PlayerControlResult.TargetOffline -> error(MessageKey("administration.sudo.target_offline"))
        PlayerControlResult.SelfTarget -> error(MessageKey("administration.sudo.self"))
        PlayerControlResult.TargetProtected -> error(MessageKey("administration.sudo.protected"))
        PlayerControlResult.RuntimeStopping -> error(MessageKey("core.error.runtime_stopping"))
        else -> error(MessageKey("administration.error.unexpected"))
    }

internal fun AdministrationMessages.socialSpy(result: SocialSpyResult): Notice =
    withAudit(
        success(
            when {
                result.enabled -> MessageKey("administration.socialspy.enabled")
                else -> MessageKey("administration.socialspy.disabled")
            }
        ),
        result.auditRecorded,
    )

internal fun AdministrationMessages.vanish(result: VanishResult): Notice =
    withAudit(
        success(
            when {
                result.enabled -> MessageKey("administration.vanish.enabled")
                else -> MessageKey("administration.vanish.disabled")
            }
        ),
        result.auditRecorded,
    )

internal fun AdministrationMessages.notifyKick(actor: String, target: String): List<Notice> =
    listOf(
        success(
            MessageKey("administration.notify.kick"),
            literal(actor),
            player(target)
        )
    )

internal fun AdministrationMessages.notifyKickAll(actor: String, count: Int): List<Notice> =
    listOf(
        success(
            MessageKey("administration.notify.kick_all"),
            literal(actor),
            count(count)
        )
    )

internal fun AdministrationMessages.notifyBan(
    actor: String,
    target: String,
    expiresAt: Instant?,
    now: Instant
): List<Notice> =
    listOf(
        when (expiresAt) {
            null ->
                success(
                    MessageKey("administration.notify.ban"),
                    literal(actor),
                    player(target)
                )

            else ->
                success(
                    MessageKey("administration.notify.ban_temp"),
                    literal(actor),
                    player(target),
                    elapsed(expiresAt, now),
                )
        }
    )

internal fun AdministrationMessages.notifyIpBan(actor: String): List<Notice> =
    listOf(
        success(
            MessageKey("administration.notify.ip_ban"),
            literal(actor)
        )
    )

internal fun AdministrationMessages.notifyMute(
    actor: String,
    target: String,
    expiresAt: Instant?,
    now: Instant
): List<Notice> =
    listOf(
        when (expiresAt) {
            null ->
                success(
                    MessageKey("administration.notify.mute"),
                    literal(actor),
                    player(target)
                )

            else ->
                success(
                    MessageKey("administration.notify.mute_temp"),
                    literal(actor),
                    player(target),
                    elapsed(expiresAt, now),
                )
        }
    )

internal fun AdministrationMessages.pendingKick(target: String): LocalizedText =
    LocalizedText(
        MessageKey("administration.pending.kick"),
        listOf(player(target))
    )

internal fun AdministrationMessages.pendingKill(target: String): LocalizedText =
    LocalizedText(
        MessageKey("administration.pending.kill"),
        listOf(player(target))
    )

internal fun AdministrationMessages.pendingSudo(target: String): LocalizedText =
    LocalizedText(
        MessageKey("administration.pending.sudo"),
        listOf(player(target))
    )

internal fun AdministrationMessages.mutedChat(): Notice =
    error(MessageKey("administration.moderation.muted_chat"))
