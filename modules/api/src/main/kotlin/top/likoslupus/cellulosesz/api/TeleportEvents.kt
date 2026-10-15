package top.likoslupus.cellulosesz.api

import java.util.*

/**
 * A stable, coarse teleport origin. New internal causes map onto an existing value or [OTHER]; the
 * enum is not expected to grow for every internal cause.
 */
public enum class TeleportOriginKind {

    HOME,
    WARP,
    SPAWN,
    REQUEST,
    DIRECT,
    ADMIN,
    JAIL,
    RANDOM,
    TERRAIN,
    OTHER,

}

/** An immutable position without any world object. */
public class ApiPosition(
    public val dimension: String,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val yaw: Float,
    public val pitch: Float,
)

/**
 * A confirmed teleport. [to] is the position the server actually committed, never the requested
 * destination. Only CellulosesZ-initiated, verified-successful moves are published.
 */
public class TeleportCommitted(
    public val subjectId: UUID,
    public val kind: TeleportOriginKind,
    public val from: ApiPosition,
    public val to: ApiPosition,
)

/** Observation callback. It runs on the server thread and must not trigger another teleport. */
public fun interface TeleportListener {

    public fun onCommitted(event: TeleportCommitted)

}

/** Subscription point for confirmed teleports. */
public fun interface TeleportEvents {

    public fun subscribe(listener: TeleportListener): Subscription

}
