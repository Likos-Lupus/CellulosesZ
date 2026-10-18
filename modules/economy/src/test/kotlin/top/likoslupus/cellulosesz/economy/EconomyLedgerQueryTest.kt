package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.service.EconomyMutationResult
import top.likoslupus.cellulosesz.economy.service.GrantIntent
import top.likoslupus.cellulosesz.economy.service.TakeIntent
import top.likoslupus.cellulosesz.economy.service.TransferIntent
import java.util.*
import kotlinx.coroutines.runBlocking

/**
 * The ledger read-side invariants: rejected writes leave every queried balance untouched, idempotency
 * conflicts never move funds, and an account can never be read as negative.
 */
internal class EconomyLedgerQueryTest : EconomyLedgerTestSupport() {

    @Test
    fun `insufficient funds leaves every balance untouched`() =
        runBlocking {
            runtime().use { runtime ->
                val service = service(runtime)
                val alice = UUID.randomUUID()
                val bob = UUID.randomUUID()
                service.ensureAccount(alice)
                service.ensureAccount(bob)
                service.grant(
                    GrantIntent(
                        actor,
                        alice,
                        Money.parse("10.00"),
                        "test",
                        "seed",
                        "seed"
                    )
                )

                val result = service.transfer(
                    TransferIntent(
                        actor,
                        alice,
                        bob,
                        Money.parse("25.00"),
                        "test",
                        "t1",
                        "pay"
                    )
                )

                assertEquals(
                    EconomyMutationResult.InsufficientFunds,
                    result
                )
                assertEquals(
                    Money.parse("10.00"),
                    service.amountOf(alice)
                )
                assertEquals(
                    Money.parse("0.00"),
                    service.amountOf(bob)
                )
            }
        }

    @Test
    fun `the same request key with a different amount conflicts`() =
        runBlocking {
            runtime().use { runtime ->
                val service = service(runtime)
                val alice = UUID.randomUUID()
                val bob = UUID.randomUUID()
                service.ensureAccount(alice)
                service.ensureAccount(bob)
                service.grant(
                    GrantIntent(
                        actor,
                        alice,
                        Money.parse("100.00"),
                        "test",
                        "seed",
                        "seed"
                    )
                )

                service.transfer(
                    TransferIntent(
                        actor,
                        alice,
                        bob,
                        Money.parse("10.00"),
                        "test",
                        "k",
                        "pay"
                    )
                )
                val conflict = service.transfer(
                    TransferIntent(
                        actor,
                        alice,
                        bob,
                        Money.parse("20.00"),
                        "test",
                        "k",
                        "pay"
                    )
                )

                assertEquals(
                    EconomyMutationResult.IdempotencyConflict,
                    conflict
                )
                assertEquals(
                    Money.parse("90.00"),
                    service.amountOf(alice)
                )
            }
        }

    @Test
    fun `a credit that would exceed the maximum is rejected atomically`() =
        runBlocking {
            runtime().use { runtime ->
                val service = service(runtime, maximum = "100.00")
                val alice = UUID.randomUUID()
                val bob = UUID.randomUUID()
                service.ensureAccount(alice)
                service.ensureAccount(bob)
                service.grant(
                    GrantIntent(
                        actor,
                        alice,
                        Money.parse("90.00"),
                        "test",
                        "sa",
                        "seed"
                    )
                )
                service.grant(
                    GrantIntent(
                        actor,
                        bob,
                        Money.parse("90.00"),
                        "test",
                        "sb",
                        "seed"
                    )
                )

                val result = service.transfer(
                    TransferIntent(
                        actor,
                        alice,
                        bob,
                        Money.parse("20.00"),
                        "test",
                        "t",
                        "pay"
                    )
                )

                assertEquals(
                    EconomyMutationResult.BalanceLimitExceeded,
                    result
                )
                assertEquals(
                    Money.parse("90.00"),
                    service.amountOf(alice)
                )
                assertEquals(
                    Money.parse("90.00"),
                    service.amountOf(bob)
                )
            }
        }

    @Test
    fun `a player account can never go negative`() =
        runBlocking {
            runtime().use { runtime ->
                val service = service(runtime)
                val alice = UUID.randomUUID()
                service.ensureAccount(alice)
                val result = service.take(
                    TakeIntent(
                        actor,
                        alice,
                        Money.parse("1.00"),
                        "test",
                        "k",
                        "admin"
                    )
                )
                assertEquals(
                    EconomyMutationResult.InsufficientFunds,
                    result
                )
                assertFalse((service.amountOf(alice)).isPositive)
            }
        }

}
