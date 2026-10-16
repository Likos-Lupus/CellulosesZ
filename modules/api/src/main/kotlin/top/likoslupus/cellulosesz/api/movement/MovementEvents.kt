package top.likoslupus.cellulosesz.api.movement

import top.likoslupus.cellulosesz.api.Subscription
import top.likoslupus.cellulosesz.api.TeleportCommitted
import top.likoslupus.cellulosesz.api.common.ApiEventListener
import java.time.Instant
import java.util.*

public enum class HomeChangeKind {

    CREATED,
    UPDATED,
    RENAMED,
    DELETED,

}

/** A committed home change. Coordinates are intentionally omitted from the general event. */
public class HomeChanged(
    public val ownerId: UUID,
    public val name: String,
    public val kind: HomeChangeKind,
    public val occurredAt: Instant,
)

public enum class WarpChangeKind {

    CREATED,
    UPDATED,
    DELETED,

}

public class WarpChanged(
    public val name: String,
    public val kind: WarpChangeKind,
    public val occurredAt: Instant,
)

public class SpawnChanged(
    public val configured: Boolean,
    public val occurredAt: Instant,
)

public enum class TeleportRequestState {

    PENDING,
    ACCEPTED,
    DENIED,
    CANCELLED,
    EXPIRED,

}

public class TeleportRequestChanged(
    public val senderId: UUID,
    public val targetId: UUID,
    public val state: TeleportRequestState,
)

/** Fact events for the Movement domain; each is delivered on the server thread. */
public interface MovementEvents {

    public fun onTeleportCommitted(
        listener: ApiEventListener<TeleportCommitted>,
    ): Subscription

    public fun onHomeChanged(
        listener: ApiEventListener<HomeChanged>,
    ): Subscription

    public fun onWarpChanged(
        listener: ApiEventListener<WarpChanged>,
    ): Subscription

    public fun onSpawnChanged(
        listener: ApiEventListener<SpawnChanged>,
    ): Subscription

    public fun onTeleportRequestChanged(
        listener: ApiEventListener<TeleportRequestChanged>,
    ): Subscription

}
