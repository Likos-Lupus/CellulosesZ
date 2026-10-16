package top.likoslupus.cellulosesz.api.extension

import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.TeleportOriginKind
import java.util.*

/** The immutable context of a teleport about to commit. It carries no mutable world/entity handle. */
public class TeleportPreflightView(
    public val playerId: UUID,
    public val origin: TeleportOriginKind,
    public val destination: ApiPosition,
)

public enum class TeleportDenialCode {

    REGION_DENIED,
    POLICY_DENIED,
    OTHER,

}

public sealed interface TeleportPolicyResult {

    public data object Allow : TeleportPolicyResult

    public data class Deny(public val reason: TeleportDenialCode) : TeleportPolicyResult

}

/**
 * A synchronous pre-commit teleport policy. It runs on the server thread, immediately before the
 * Minecraft teleport call, and may only allow or deny — it can never rewrite the destination, grant
 * privilege, or perform IO. A thrown exception is treated as a denial (fail closed).
 *
 * It only governs teleports CellulosesZ itself initiates; it cannot intercept arbitrary third-party
 * teleports.
 */
public fun interface TeleportPolicyChecker {

    public fun check(request: TeleportPreflightView): TeleportPolicyResult

}
