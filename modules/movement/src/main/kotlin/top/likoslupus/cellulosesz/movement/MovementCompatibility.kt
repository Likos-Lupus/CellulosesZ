package top.likoslupus.cellulosesz.movement

import java.util.*

/**
 * Minecraft-free origin of a CellulosesZ teleport, exposed for compatibility hooks. Internal causes
 * map onto this narrower set so a hook never depends on the teleport domain's internal enum.
 */
public enum class TeleportOrigin {

    HOME,
    WARP,
    SPAWN,
    BACK,
    REQUEST,
    DIRECT,
    ADMIN,
    JAIL,
    RANDOM,
    TERRAIN,
    OTHER,

}

/** An immutable position for a compatibility hook; no world object is ever exposed. */
public class TeleportEndpoint(
    public val dimension: String,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val yaw: Float,
    public val pitch: Float,
)

/** Immutable description of a fully resolved teleport that is about to be committed. */
public class PreparedTeleport(
    public val subjectId: UUID,
    public val origin: TeleportOrigin,
    public val destination: TeleportEndpoint,
)

/** Controlled veto reasons; a raw exception message is never shown to a player. */
public enum class TeleportVetoReason {

    REGION_DENIED,
    PLATFORM_POLICY,
    OTHER,

}

/** A pre-commit decision. It can only allow or deny; it can never rewrite the destination. */
public sealed interface TeleportPreflightDecision {

    public data object Allow : TeleportPreflightDecision

    public data class Deny(public val reason: TeleportVetoReason) : TeleportPreflightDecision

}

/**
 * Final synchronous veto, evaluated on the server thread immediately before the Minecraft teleport
 * call, after the destination has been fully resolved. Implementations must be fast, must not
 * suspend or perform IO, and must not trigger another teleport. Only CellulosesZ-initiated moves are
 * offered to it.
 */
public fun interface TeleportPreflight {

    public fun evaluate(request: PreparedTeleport): TeleportPreflightDecision

    public companion object {

        /** The default: never veto, so an unconfigured server behaves exactly as before. */
        public val AllowAll: TeleportPreflight =
            TeleportPreflight { TeleportPreflightDecision.Allow }

    }

}

/** Immutable, read-only description of a committed teleport. */
public class CommittedTeleport(
    public val subjectId: UUID,
    public val origin: TeleportOrigin,
    public val from: TeleportEndpoint,
    public val to: TeleportEndpoint,
)

/**
 * Read-only post-commit observer. It runs on the server thread after the move is confirmed and the
 * cooldown is recorded; it cannot change the committed result, and a failure is logged without
 * affecting other observers or the teleport itself.
 */
public fun interface TeleportCommittedObserver {

    public fun onCommitted(event: CommittedTeleport)

    public companion object {

        public val None: TeleportCommittedObserver = TeleportCommittedObserver { }

    }

}
