package top.likoslupus.cellulosesz.administration.moderation.command

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.administration.moderation.ban.BanResult
import top.likoslupus.cellulosesz.administration.moderation.ban.IpBanResult
import top.likoslupus.cellulosesz.administration.moderation.ban.IpUnbanResult
import top.likoslupus.cellulosesz.administration.moderation.ban.UnbanResult
import top.likoslupus.cellulosesz.administration.moderation.kick.KickAllResult
import top.likoslupus.cellulosesz.administration.moderation.kick.KickResult
import top.likoslupus.cellulosesz.administration.moderation.mute.Mute
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteResult
import top.likoslupus.cellulosesz.administration.moderation.mute.UnmuteResult
import top.likoslupus.cellulosesz.core.text.Messages
import java.time.Duration
import java.time.Instant

/** Owns moderation feedback text, including the compact remaining-duration formatter. */
internal object ModerationFeedback {

    fun formatRemaining(expiresAt: Instant, now: Instant): String {
        val total = Duration.between(now, expiresAt).seconds.coerceAtLeast(0)
        val days = total / 86_400
        val hours = (total % 86_400) / 3_600
        val minutes = (total % 3_600) / 60
        val seconds = total % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun expiry(expiresAt: Instant?, now: Instant): String =
        when (expiresAt) {
            null -> "permanently"
            else -> "for ${formatRemaining(expiresAt, now)}"
        }

    fun auditNote(recorded: Boolean): String =
        when {
            recorded -> ""
            else -> " (audit record could not be written)"
        }

    fun kick(result: KickResult): Component =
        prefixed(
            when (result) {
                is KickResult.Kicked -> "kicked ${result.target.name}${auditNote(result.auditRecorded)}"
                KickResult.TargetOffline -> "that player is not online"
                KickResult.SelfTarget -> "you cannot kick yourself"
                KickResult.TargetProtected -> "that player is protected from this action"
            }
        )

    fun kickAll(result: KickAllResult): Component =
        prefixed(
            buildString {
                append("kicked ${result.kicked} player(s)")
                if (result.protected > 0) append("; ${result.protected} protected player(s) skipped")
                if (result.skippedIssuer) append("; issuer excluded")
            }
        )

    fun ban(result: BanResult, now: Instant): Component =
        prefixed(
            when (result) {
                is BanResult.Applied -> "banned ${result.target.name} ${
                    expiry(
                        result.expiresAt,
                        now
                    )
                }${auditNote(result.auditRecorded)}"

                BanResult.TargetUnknown -> "player is not known to this server"
                BanResult.SelfTarget -> "you cannot ban yourself"
                BanResult.TargetProtected -> "that player is protected from this action"
                BanResult.AlreadyBanned -> "that player is already banned"
                BanResult.DurationTooLong -> "temporary duration exceeds the configured maximum"
                BanResult.RuntimeStopping -> "runtime is shutting down"
            }
        )

    fun unban(result: UnbanResult): Component =
        prefixed(
            when (result) {
                is UnbanResult.Unbanned -> "unbanned ${result.target.name}${auditNote(result.auditRecorded)}"
                UnbanResult.TargetUnknown -> "player is not known to this server"
                UnbanResult.NotBanned -> "that player is not banned"
                UnbanResult.RuntimeStopping -> "runtime is shutting down"
            }
        )

    fun ipBan(result: IpBanResult, now: Instant): Component =
        prefixed(
            when (result) {
                is IpBanResult.Applied -> "banned IP ${result.address} ${
                    expiry(
                        result.expiresAt,
                        now
                    )
                }${auditNote(result.auditRecorded)}"

                IpBanResult.InvalidAddress -> "provide a valid IP literal or an online player"
                IpBanResult.TargetProtected -> "that address belongs to a protected player"
                IpBanResult.AlreadyBanned -> "that address is already banned"
                IpBanResult.DurationTooLong -> "temporary duration exceeds the configured maximum"
                IpBanResult.RuntimeStopping -> "runtime is shutting down"
            }
        )

    fun ipUnban(result: IpUnbanResult): Component =
        prefixed(
            when (result) {
                is IpUnbanResult.Unbanned -> "unbanned IP ${result.address}${auditNote(result.auditRecorded)}"
                IpUnbanResult.InvalidAddress -> "provide a valid IP literal or an online player"
                IpUnbanResult.NotBanned -> "that address is not banned"
                IpUnbanResult.RuntimeStopping -> "runtime is shutting down"
            }
        )

    fun mute(result: MuteResult, now: Instant): Component =
        prefixed(
            when (result) {
                is MuteResult.Applied -> "muted ${result.target.name} ${
                    expiry(
                        result.expiresAt,
                        now
                    )
                }${auditNote(result.auditRecorded)}"

                MuteResult.TargetUnknown -> "player is not known to this server"
                MuteResult.SelfTarget -> "you cannot mute yourself"
                MuteResult.TargetProtected -> "that player is protected from this action"
                MuteResult.AlreadyMuted -> "that player is already muted"
                MuteResult.DurationTooLong -> "temporary duration exceeds the configured maximum"
                MuteResult.StorageUnavailable -> "moderation storage is unavailable"
                MuteResult.RuntimeStopping -> "runtime is shutting down"
            }
        )

    fun unmute(result: UnmuteResult): Component =
        prefixed(
            when (result) {
                is UnmuteResult.Unmuted -> "unmuted ${result.target.name}${auditNote(result.auditRecorded)}"
                UnmuteResult.TargetUnknown -> "player is not known to this server"
                UnmuteResult.NotMuted -> "that player is not muted"
                UnmuteResult.StorageUnavailable -> "moderation storage is unavailable"
                UnmuteResult.RuntimeStopping -> "runtime is shutting down"
            }
        )

    fun muteInfo(mute: Mute, now: Instant): Component =
        prefixed(
            "mute: player=${mute.playerName} " +
                    "reason=${mute.reason} " +
                    "issuedBy=${mute.actor.name} " +
                    "issuedAt=${mute.issuedAt} " +
                    "expires=${
                        mute.expiresAt?.let {
                            formatRemaining(it, now)
                        } ?: "never"
                    }"
        )

    private fun prefixed(message: String): Component =
        Messages.prefixed(message)

}
