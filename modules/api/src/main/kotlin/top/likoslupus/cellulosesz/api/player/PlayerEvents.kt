package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import java.util.UUID

/** Why an AFK state changed, mapped from the domain's own transition reason. */
public enum class AfkChangeCause {

    MANUAL,
    ACTIVITY,
    TIMEOUT,
    OTHER,

}

public class PlayerAfkChanged(
    public val playerId: UUID,
    public val afk: Boolean,
    public val cause: AfkChangeCause,
)

public class PlayerNicknameChanged(
    public val playerId: UUID,
    public val previous: String?,
    public val current: String?,
)

/** CellulosesZ's own view of a player's online state (not a claim about every other mod's view). */
public class PlayerPresenceChanged(
    public val playerId: UUID,
    public val online: Boolean,
)

/** Fact events published from the domain's own commit points, dispatched on the server thread. */
public interface PlayerEvents {

    public fun onAfkChanged(
        listener: ApiEventListener<PlayerAfkChanged>,
    ): Subscription

    public fun onNicknameChanged(
        listener: ApiEventListener<PlayerNicknameChanged>,
    ): Subscription

    public fun onPresenceChanged(
        listener: ApiEventListener<PlayerPresenceChanged>,
    ): Subscription

}
