package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.service.*
import java.util.*
import kotlinx.coroutines.runBlocking

/**
 * The ledger write path: opening balances, transfers, idempotent receipts, `set` and `take` all
 * commit exactly once and leave the expected balances behind.
 */
internal class EconomyLedgerCommitTest : EconomyLedgerTestSupport() {

    @Test
    fun `opening balance is applied exactly once`() =
        runBlocking {
            runtime().use { runtime ->
                val service = service(runtime, starting = "100.00")
                val alice = UUID.randomUUID()

                assertTrue(service.ensureAccount(alice))
                assertEquals(
                    Money.parse("100.00"),
                    service.amountOf(alice)
                )

                assertTrue(service.ensureAccount(alice))
                assertEquals(
                    Money.parse("100.00"),
                    service.amountOf(alice)
                )
            }
        }

    @Test
    fun `transfer moves funds atomically and conserves the total`() =
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

                assertInstanceOf(
                    EconomyMutationResult.Committed::class.java,
                    result
                )
                assertEquals(
                    Money.parse("75.00"),
                    service.amountOf(alice)
                )
                assertEquals(
                    Money.parse("25.00"),
                    service.amountOf(bob)
                )
            }
        }

    @Test
    fun `a repeated request key returns the original receipt without a second move`() =
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

                val first = service.transfer(
                    TransferIntent(
                        actor,
                        alice,
                        bob,
                        Money.parse("25.00"),
                        "test",
                        "same",
                        "pay"
                    )
                )
                val second = service.transfer(
                    TransferIntent(
                        actor,
                        alice,
                        bob,
                        Money.parse("25.00"),
                        "test",
                        "same",
                        "pay"
                    )
                )

                assertInstanceOf(
                    EconomyMutationResult.Committed::class.java,
                    first
                )
                assertInstanceOf(
                    EconomyMutationResult.AlreadyCommitted::class.java,
                    second
                )
                assertEquals(
                    Money.parse("75.00"),
                    service.amountOf(alice)
                )
                assertEquals(
                    Money.parse("25.00"),
                    service.amountOf(bob)
                )
            }
        }

    @Test
    fun `set to a new target records the delta and set to the same target is a durable no-change`() =
        runBlocking {
            runtime().use { runtime ->
                val service = service(runtime)
                val alice = UUID.randomUUID()
                service.ensureAccount(alice)
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

                val changed = service.set(
                    SetIntent(
                        actor,
                        alice,
                        Money.parse("40.00"),
                        "test",
                        "s1",
                        "admin"
                    )
                )
                assertInstanceOf(
                    EconomyMutationResult.Committed::class.java,
                    changed
                )
                assertEquals(
                    Money.parse("40.00"),
                    service.amountOf(alice)
                )

                val noChange = service.set(
                    SetIntent(
                        actor,
                        alice,
                        Money.parse("40.00"),
                        "test",
                        "s2",
                        "admin"
                    )
                )
                assertInstanceOf(
                    EconomyMutationResult.NoChange::class.java,
                    noChange
                )
                assertEquals(
                    Money.parse("40.00"),
                    service.amountOf(alice)
                )

                val replay = service.set(
                    SetIntent(
                        actor,
                        alice,
                        Money.parse("40.00"),
                        "test",
                        "s2",
                        "admin"
                    )
                )
                assertTrue(replay is EconomyMutationResult.AlreadyCommitted)
            }
        }

    @Test
    fun `take reclaims into the system and rejects an over-draw`() =
        runBlocking {
            runtime().use { runtime ->
                val service = service(runtime)
                val alice = UUID.randomUUID()
                service.ensureAccount(alice)
                service.grant(
                    GrantIntent(
                        actor,
                        alice,
                        Money.parse("30.00"),
                        "test",
                        "seed",
                        "seed"
                    )
                )

                assertInstanceOf(
                    EconomyMutationResult.Committed::class.java,
                    service.take(
                        TakeIntent(
                            actor,
                            alice,
                            Money.parse("10.00"),
                            "test",
                            "k1",
                            "admin"
                        )
                    ),
                )
                assertEquals(
                    Money.parse("20.00"),
                    service.amountOf(alice)
                )

                assertEquals(
                    EconomyMutationResult.InsufficientFunds,
                    service.take(
                        TakeIntent(
                            actor,
                            alice,
                            Money.parse("50.00"),
                            "test",
                            "k2",
                            "admin"
                        )
                    ),
                )
                assertEquals(
                    Money.parse("20.00"),
                    service.amountOf(alice)
                )
            }
        }

}
