package top.likoslupus.cellulosesz.api.moderation

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import java.time.Instant
import java.util.*

public class PlayerMuted(
    public val playerId: UUID,
    public val until: Instant?,
    public val actionId: String,
)

public class PlayerUnmuted(
    public val playerId: UUID,
    public val actionId: String,
)

public class PlayerConfined(
    public val playerId: UUID,
    public val actionId: String,
)

public class PlayerReleased(
    public val playerId: UUID,
    public val actionId: String,
)

public class PlayerVanishStateChanged(
    public val playerId: UUID,
    public val vanished: Boolean,
)

/**
 * Moderation fact events. They describe CellulosesZ's own committed actions only; a vanilla ban or
 * another mod's permission change is not a CellulosesZ event.
 */
public interface ModerationEvents {

    public fun onMuted(
        listener: ApiEventListener<PlayerMuted>,
    ): Subscription

    public fun onUnmuted(
        listener: ApiEventListener<PlayerUnmuted>,
    ): Subscription

    public fun onConfined(
        listener: ApiEventListener<PlayerConfined>,
    ): Subscription

    public fun onReleased(
        listener: ApiEventListener<PlayerReleased>,
    ): Subscription

    public fun onVanishStateChanged(
        listener: ApiEventListener<PlayerVanishStateChanged>,
    ): Subscription

}
