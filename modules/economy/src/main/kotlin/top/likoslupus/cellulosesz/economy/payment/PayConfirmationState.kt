package top.likoslupus.cellulosesz.economy.payment

import top.likoslupus.cellulosesz.economy.money.Money
import java.util.*

/** What the `/pay` confirmation step decided for the current invocation. */
internal sealed interface PayConfirmationDecision {

    /** The identical command was repeated; the caller may run the payment now. */
    data object Execute : PayConfirmationDecision

    /** The command was stored as pending; the caller must repeat it to confirm. */
    data class Confirm(
        val receiverName: String,
        val amount: Money,
    ) : PayConfirmationDecision

}

/**
 * The session-only two-step `/pay` confirmation. Confined to the server thread, so a plain map is
 * used with no locking. The pending entry is keyed by payer: repeating the identical
 * `(receiver, amount)` executes, any different command replaces the pending entry, and a toggle,
 * quit or server stop clears it.
 */
internal class PayConfirmationState {

    private data class Pending(
        val receiverId: UUID,
        val amount: Money,
    )

    private val pending = HashMap<UUID, Pending>()

    fun request(
        payerId: UUID,
        receiverId: UUID,
        amount: Money,
        receiverName: String,
    ): PayConfirmationDecision {
        val current = pending[payerId]
        if (current != null
            && current.receiverId == receiverId
            && current.amount.compareTo(amount) == 0
        ) {
            pending.remove(payerId)
            return PayConfirmationDecision.Execute
        }
        pending[payerId] = Pending(receiverId, amount)
        return PayConfirmationDecision.Confirm(receiverName, amount)
    }

    fun clear(payerId: UUID) {
        pending.remove(payerId)
    }

    fun clearAll() {
        pending.clear()
    }

}
