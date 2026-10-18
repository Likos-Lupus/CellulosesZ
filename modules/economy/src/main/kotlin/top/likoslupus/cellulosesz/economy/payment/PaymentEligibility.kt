package top.likoslupus.cellulosesz.economy.payment

import java.util.*

/**
 * The economy's payment-eligibility seam. It can only ever *remove* eligibility: the default
 * implementation allows every pair, and the composition root replaces it with the communication
 * ignore-list query. The check is server-thread confined and must not block; implementations read
 * only cached state.
 */
public fun interface PaymentEligibility {

    /** Whether [payerId] may pay [receiverId] under the external policy. */
    public fun allows(payerId: UUID, receiverId: UUID): Boolean

    public companion object {

        /** The permissive default: every pair is eligible. */
        public val AllowAll: PaymentEligibility = PaymentEligibility { _, _ -> true }

    }

}

/** The typed reason `/pay` refused a pair before the ledger was touched. */
internal sealed interface PayEligibilityDecision {

    data object Allowed : PayEligibilityDecision

    /** The receiver turned payments off and the payer lacks the bypass. */
    data object ReceiverDisabled : PayEligibilityDecision

    /** The receiver ignores the payer and the server excludes ignore lists from payments. */
    data object ReceiverBlocked : PayEligibilityDecision

}

/**
 * The single rule behind `/paytoggle` and `excludesIgnoreList`, kept pure so it can be unit-tested
 * without Minecraft or storage.
 */
internal object PayEligibilityGate {

    fun decide(
        receiverAcceptsPay: Boolean,
        payerBypasses: Boolean,
        receiverExcluded: Boolean,
    ): PayEligibilityDecision =
        when {
            !payerBypasses && !receiverAcceptsPay -> PayEligibilityDecision.ReceiverDisabled
            receiverExcluded -> PayEligibilityDecision.ReceiverBlocked
            else -> PayEligibilityDecision.Allowed
        }

}
