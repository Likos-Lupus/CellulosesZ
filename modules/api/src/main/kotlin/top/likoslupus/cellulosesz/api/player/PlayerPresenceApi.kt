package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * A read-only snapshot of what CellulosesZ knows about a player. It never contains an IP address, raw
 * UUID mapping cache or moderation data. [lastKnownName] is the last name CellulosesZ saw, not a live
 * Mojang lookup. [playtime] is a JDK [Duration], not an internal type.
 */
public class PlayerPresenceView(
    public val playerId: UUID,
    public val lastKnownName: String,
    public val online: Boolean,
    public val afk: Boolean,
    public val nickname: String?,
    public val firstSeen: Instant?,
    public val lastSeen: Instant?,
    public val playtime: Duration?,
)

/** The result of a persisted profile query. [Unknown] is distinct from "known but offline". */
public sealed interface PlayerProfileOutcome {

    public data class Found(public val profile: PlayerPresenceView) : PlayerProfileOutcome

    public data object Unknown : PlayerProfileOutcome

}

/**
 * Cached, non-blocking presence reads plus an asynchronous persisted-profile query.
 *
 * [online] and [afk] read an immutable, safely published snapshot and never touch storage. For [afk],
 * a `null` value means "no current basis to answer" — an offline player must not be reported as
 * `online=false, afk=false` as if that were a meaningful live state.
 */
public interface PlayerPresenceApi {

    public fun online(playerId: UUID): ApiQuery<Boolean>

    public fun afk(playerId: UUID): ApiQuery<Boolean?>

    public fun profile(
        playerId: UUID,
        callback: ApiCallback<PlayerProfileOutcome>,
    ): ApiRequestHandle

}
