package top.likoslupus.cellulosesz.economy.payment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.economy.money.Money
import java.util.*

/** The session-only two-step `/pay` confirmation state machine. */
class PayConfirmationStateTest {

    private val payer = UUID.randomUUID()
    private val receiver = UUID.randomUUID()
    private val other = UUID.randomUUID()

    @Test
    fun `the first request is pending`() {
        val state = PayConfirmationState()

        assertInstanceOf(
            PayConfirmationDecision.Confirm::class.java,
            state.request(
                payer,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
    }

    @Test
    fun `repeating the identical command executes and consumes the pending entry`() {
        val state = PayConfirmationState()
        state.request(
            payer,
            receiver,
            Money.parse("25.00"),
            "Alex"
        )

        assertEquals(
            PayConfirmationDecision.Execute,
            state.request(
                payer,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
        // A third identical command starts a fresh confirmation.
        assertInstanceOf(
            PayConfirmationDecision.Confirm::class.java,
            state.request(
                payer,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
    }

    @Test
    fun `a different amount replaces the pending entry`() {
        val state = PayConfirmationState()
        state.request(
            payer,
            receiver,
            Money.parse("25.00"),
            "Alex"
        )

        val replaced = assertInstanceOf(
            PayConfirmationDecision.Confirm::class.java,
            state.request(
                payer,
                receiver,
                Money.parse("10.00"),
                "Alex"
            ),
        )
        assertEquals(Money.parse("10.00"), replaced.amount)

        // The first amount is no longer pending, so it only re-arms.
        assertInstanceOf(
            PayConfirmationDecision.Confirm::class.java,
            state.request(
                payer,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
        assertEquals(
            PayConfirmationDecision.Execute,
            state.request(
                payer,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
    }

    @Test
    fun `a different receiver replaces the pending entry`() {
        val state = PayConfirmationState()
        state.request(
            payer,
            receiver,
            Money.parse("25.00"),
            "Alex"
        )

        assertInstanceOf(
            PayConfirmationDecision.Confirm::class.java,
            state.request(
                payer,
                other,
                Money.parse("25.00"),
                "Bob"
            ),
        )
        assertEquals(
            PayConfirmationDecision.Execute,
            state.request(
                payer,
                other,
                Money.parse("25.00"),
                "Bob"
            ),
        )
    }

    @Test
    fun `amounts compare by value regardless of scale`() {
        val state = PayConfirmationState()
        state.request(
            payer,
            receiver,
            Money.parse("25.0"),
            "Alex"
        )

        assertEquals(
            PayConfirmationDecision.Execute,
            state.request(
                payer,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
    }

    @Test
    fun `clear drops one payer and clearAll drops everyone`() {
        val state = PayConfirmationState()
        state.request(
            payer,
            receiver,
            Money.parse("25.00"),
            "Alex"
        )
        state.request(
            other,
            receiver,
            Money.parse("25.00"),
            "Alex"
        )

        state.clear(payer)
        state.clearAll()

        assertInstanceOf(
            PayConfirmationDecision.Confirm::class.java,
            state.request(
                payer,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
    }

    @Test
    fun `different payers do not share pending state`() {
        val state = PayConfirmationState()
        state.request(
            payer,
            receiver,
            Money.parse("25.00"),
            "Alex"
        )

        assertInstanceOf(
            PayConfirmationDecision.Confirm::class.java,
            state.request(
                other,
                receiver,
                Money.parse("25.00"),
                "Alex"
            ),
        )
    }

}
