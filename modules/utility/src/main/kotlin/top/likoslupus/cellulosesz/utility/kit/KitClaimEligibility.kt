package top.likoslupus.cellulosesz.utility.kit

import java.util.UUID

/**
 * A kit-claim eligibility decision. It can only add a restriction; it never grants a claim that the
 * kit's own Once/Cooldown/inventory rules would refuse.
 */
public sealed interface KitEligibilityDecision {

    public data object Allow : KitEligibilityDecision

    /** A machine-readable reason key owned by the contributing addon. */
    public data class Deny(public val reasonKey: String) : KitEligibilityDecision

}

/**
 * A synchronous, Minecraft-free claim guard, evaluated on the server thread before the inventory
 * precheck and the durable reservation. Multiple checkers are composed in `application`; a throwing
 * checker denies the claim (fail closed).
 */
public fun interface KitClaimEligibility {

    public fun evaluate(playerId: UUID, kitName: String): KitEligibilityDecision

    public companion object {

        public val AllowAll: KitClaimEligibility =
            KitClaimEligibility { _, _ -> KitEligibilityDecision.Allow }

    }

}
