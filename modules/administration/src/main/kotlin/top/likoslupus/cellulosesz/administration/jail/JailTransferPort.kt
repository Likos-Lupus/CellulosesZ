package top.likoslupus.cellulosesz.administration.jail

import java.util.*

/**
 * Where a jail transfer is headed. Defined in administration without any movement import so the two
 * feature modules never depend on each other; the composition root adapts it to movement.
 */
public data class JailDestination(
    public val dimension: String,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val yaw: Float,
    public val pitch: Float,
)

/** Outcome of a jail transfer. Expected failures are values. */
public sealed interface JailTransferResult {

    public data object Transferred : JailTransferResult

    public data object PlayerOffline : JailTransferResult

    public data class Failed(public val reason: String) : JailTransferResult

}

/**
 * Why a jail transfer happens. It selects the movement teleport cause across the module boundary so
 * a confinement move is safety-checked, a correction re-anchors an escape, and a release returns the
 * player safely.
 */
public enum class JailTransferKind {

    CONFINEMENT,
    CORRECTION,
    RELEASE,

}

/**
 * Cross-module port: teleports a player into a jail or releases them to the server spawn. The
 * implementation lives in the composition root and delegates to movement's single teleport path.
 */
public interface JailTransferPort {

    public suspend fun transfer(
        playerId: UUID,
        destination: JailDestination,
        kind: JailTransferKind,
    ): JailTransferResult

    public suspend fun release(playerId: UUID): JailTransferResult

}
