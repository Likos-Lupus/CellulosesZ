package top.likoslupus.cellulosesz.api.movement

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.TeleportOriginKind
import top.likoslupus.cellulosesz.api.common.ApiCallback
import top.likoslupus.cellulosesz.api.common.ApiRequestHandle
import java.time.Duration
import java.util.UUID

/** A travel destination that CellulosesZ resolves and validates itself. */
public sealed interface TeleportTarget {

    public data class Home(public val name: String?) : TeleportTarget

    public data class Warp(public val name: String) : TeleportTarget

    public data object Spawn : TeleportTarget

    public data class Player(public val playerId: UUID) : TeleportTarget

}

/**
 * An audit/display classification of where a request came from. It is **not** a privilege switch: it
 * can never select an internal bypass cause (admin/jail) or relax cooldown/history policy.
 */
public enum class TeleportRequestSource {

    ADDON_MENU,
    ADDON_QUEST,
    ADDON_OTHER,

}

public class PlayerTeleportRequest(
    public val playerId: UUID,
    public val target: TeleportTarget,
    public val source: TeleportRequestSource,
)

/** The receipt of a committed teleport. [to] is the position the server actually committed. */
public class TeleportReceiptView(
    public val playerId: UUID,
    public val from: ApiPosition,
    public val to: ApiPosition,
    public val origin: TeleportOriginKind,
)

/** The full outcome of a requested teleport, mapped from the internal coordinator result. */
public sealed interface TeleportActionOutcome {

    public data class Committed(public val receipt: TeleportReceiptView) : TeleportActionOutcome

    public data object PlayerOffline : TeleportActionOutcome

    public data object TargetOffline : TeleportActionOutcome

    public data object DestinationNotFound : TeleportActionOutcome

    public data object Denied : TeleportActionOutcome

    public data object Confined : TeleportActionOutcome

    public data object UnsafeDestination : TeleportActionOutcome

    public data object PlatformRejected : TeleportActionOutcome

    public data class Cooldown(public val remaining: Duration) : TeleportActionOutcome

    public data object AlreadyPending : TeleportActionOutcome

    public data object Cancelled : TeleportActionOutcome

    public data object OtherFailure : TeleportActionOutcome

}

/**
 * The one and only way for an addon to move a player. Every request is resolved and executed by the
 * existing TeleportCoordinator, so permission, safety, cooldown, history and preflight policy are
 * identical to the commands. An addon must never call the Minecraft teleport API directly.
 */
public interface TeleportApi {

    public fun travel(
        request: PlayerTeleportRequest,
        callback: ApiCallback<TeleportActionOutcome>,
    ): ApiRequestHandle

}
