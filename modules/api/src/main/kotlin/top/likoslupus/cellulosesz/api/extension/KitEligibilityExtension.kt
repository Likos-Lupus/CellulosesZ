package top.likoslupus.cellulosesz.api.extension

import java.util.*

public class KitClaimContextView(
    public val playerId: UUID,
    public val kitName: String,
)

public sealed interface KitEligibilityDecision {

    public data object Allow : KitEligibilityDecision

    /** A machine-readable reason key the addon has registered; never untrusted rich text. */
    public data class Deny(public val reasonKey: String) : KitEligibilityDecision

}

/**
 * A synchronous kit-eligibility guard. It can only add restrictions: it never bypasses a kit's own
 * Once/Cooldown, inventory or reservation rules. It runs on the server thread before reservation and
 * must not perform IO. A thrown exception denies the claim.
 */
public fun interface KitEligibilityChecker {

    public fun check(request: KitClaimContextView): KitEligibilityDecision

}
