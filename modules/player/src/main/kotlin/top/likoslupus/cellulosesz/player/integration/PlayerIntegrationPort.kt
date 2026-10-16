package top.likoslupus.cellulosesz.player.integration

import java.time.Duration
import java.time.Instant
import java.util.*

/** An immutable, Minecraft-free player profile snapshot for the public API. */
public class PlayerProfileSnapshot(
    public val playerId: UUID,
    public val lastKnownName: String,
    public val online: Boolean,
    public val firstSeen: Instant?,
    public val lastSeen: Instant?,
    public val playtime: Duration?,
)

public sealed interface PlayerProfileResult {

    public data class Found(public val snapshot: PlayerProfileSnapshot) : PlayerProfileResult

    public data object Unknown : PlayerProfileResult

    public data object StorageUnavailable : PlayerProfileResult

}

public sealed interface NicknameResult {

    public data class Current(public val name: String?) : NicknameResult

    public data object Unknown : NicknameResult

    public data object StorageUnavailable : NicknameResult

}

public sealed interface NicknameChangeResult {

    public data class Changed(public val name: String?) : NicknameChangeResult

    public data object Disabled : NicknameChangeResult

    public data object InvalidName : NicknameChangeResult

    public data object NameTaken : NicknameChangeResult

    public data object Reserved : NicknameChangeResult

    public data object StorageUnavailable : NicknameChangeResult

}

/** Why an AFK state changed. Coarse on purpose: it never exposes internal tick detail. */
public enum class PlayerAfkCause {

    MANUAL,
    ACTIVITY,
    TIMEOUT,
    OTHER,

}

/** Committed player-domain facts the application adapter forwards to public listeners. */
public interface PlayerDomainObserver {

    public fun onAfkChanged(
        playerId: UUID,
        afk: Boolean,
        cause: PlayerAfkCause
    )

    public fun onNicknameChanged(
        playerId: UUID,
        previous: String?,
        current: String?
    )

    public fun onPresenceChanged(playerId: UUID, online: Boolean)

    public companion object {

        public val None: PlayerDomainObserver = object : PlayerDomainObserver {
            override fun onAfkChanged(
                playerId: UUID,
                afk: Boolean,
                cause: PlayerAfkCause
            ) =
                Unit

            override fun onNicknameChanged(
                playerId: UUID,
                previous: String?,
                current: String?
            ) =
                Unit

            override fun onPresenceChanged(playerId: UUID, online: Boolean) =
                Unit
        }

    }

}

/**
 * Narrow integration surface for the Player domain. It exposes only the operations the public API
 * needs; it never exposes [top.likoslupus.cellulosesz.player.afk.AfkService] or the nickname
 * internals. The cheap [online]/[afk] reads use immutable snapshots maintained on the server thread.
 */
public interface PlayerIntegrationPort {

    public fun online(playerId: UUID): Boolean

    public fun afk(playerId: UUID): Boolean?

    public suspend fun profile(playerId: UUID): PlayerProfileResult

    public suspend fun nickname(playerId: UUID): NicknameResult

    public suspend fun changeOwnNickname(actorId: UUID, newName: String?): NicknameChangeResult

}
