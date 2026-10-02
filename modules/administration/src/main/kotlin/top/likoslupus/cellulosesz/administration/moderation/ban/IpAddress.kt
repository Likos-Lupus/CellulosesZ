package top.likoslupus.cellulosesz.administration.moderation.ban

import com.google.common.net.InetAddresses

/**
 * A validated IP literal. Parsing is literal-only and never resolves DNS; the stored value is the
 * same canonical text produced by [net.minecraft.server.level.ServerPlayer.getIpAddress], so it
 * matches the native ban list and online-player comparisons.
 *
 * IPv6 is accepted and canonicalized, but note that vanilla `IpBanList` cannot extract an IPv6
 * address on the login path, so IPv6 IP bans only disconnect currently-online players (they do not
 * block a reconnect). Account bans are unaffected.
 */
@JvmInline
internal value class IpAddress private constructor(val value: String) {

    companion object {

        fun parse(raw: String): IpAddress? {
            val value = raw.trim()
            return when {
                value.isEmpty() -> null
                else -> try {
                    IpAddress(
                        InetAddresses.toAddrString(
                            InetAddresses.forString(value)
                        )
                    )
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }

    }

}
