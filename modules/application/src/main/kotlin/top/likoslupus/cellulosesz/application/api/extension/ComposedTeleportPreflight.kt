package top.likoslupus.cellulosesz.application.api.extension

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.ApiPosition
import top.likoslupus.cellulosesz.api.extension.TeleportDenialCode
import top.likoslupus.cellulosesz.api.extension.TeleportPolicyResult
import top.likoslupus.cellulosesz.api.extension.TeleportPreflightView
import top.likoslupus.cellulosesz.application.api.toApiKind
import top.likoslupus.cellulosesz.movement.PreparedTeleport
import top.likoslupus.cellulosesz.movement.TeleportPreflight
import top.likoslupus.cellulosesz.movement.TeleportPreflightDecision
import top.likoslupus.cellulosesz.movement.TeleportVetoReason

/**
 * Composes every addon teleport policy into the single [TeleportPreflight] the movement feature is
 * constructed with. It is fail-closed: a policy that throws denies the move and is logged with its
 * addon id. It can only allow or deny — it never rewrites the destination.
 */
internal class ComposedTeleportPreflight(
    private val extensions: ApplicationExtensionHub,
) : TeleportPreflight {

    override fun evaluate(request: PreparedTeleport): TeleportPreflightDecision {
        val policies = extensions.teleportPolicies()
        if (policies.isEmpty()) {
            return TeleportPreflightDecision.Allow
        }

        val view = request.toView()
        policies.forEach { policy ->
            val result = try {
                policy.check(view)
            } catch (failure: Throwable) {
                LOGGER.warn(
                    "teleport_policy_failed subject={} type={}",
                    request.subjectId,
                    failure.javaClass.simpleName,
                    failure,
                )
                return TeleportPreflightDecision.Deny(TeleportVetoReason.OTHER)
            }

            when (result) {
                TeleportPolicyResult.Allow ->
                    Unit

                is TeleportPolicyResult.Deny ->
                    return TeleportPreflightDecision.Deny(result.reason.toVetoReason())
            }
        }
        return TeleportPreflightDecision.Allow
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ComposedTeleportPreflight::class.java)

    }

}

private fun PreparedTeleport.toView(): TeleportPreflightView =
    TeleportPreflightView(
        playerId = subjectId,
        origin = origin.toApiKind(),
        destination = ApiPosition(
            destination.dimension,
            destination.x,
            destination.y,
            destination.z,
            destination.yaw,
            destination.pitch,
        ),
    )

private fun TeleportDenialCode.toVetoReason(): TeleportVetoReason =
    when (this) {
        TeleportDenialCode.REGION_DENIED -> TeleportVetoReason.REGION_DENIED

        TeleportDenialCode.POLICY_DENIED,
        TeleportDenialCode.OTHER -> TeleportVetoReason.OTHER
    }
