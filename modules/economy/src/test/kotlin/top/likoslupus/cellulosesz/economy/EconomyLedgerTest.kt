package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.ledger.ActorKind
import top.likoslupus.cellulosesz.economy.ledger.JdbcEconomyRepository
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.service.*
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking

class EconomyLedgerTest {

    @TempDir
    lateinit var directory: Path

    private val actor = EconomyActor(ActorKind.INTERNAL, "test")

    private fun runtime(): HikariDatabaseRuntime {
        val descriptor = DatabaseDescriptorResolver.resolve(
            DatabaseSettings(
                sqlite = SqliteSettings(
                    path = directory.resolve("cz.db").toString()
                )
            ),
            directory,
        )
        val runtime = HikariDatabaseRuntime.create(descriptor)
        SchemaInitializer.initializeBlocking(runtime, listOf(EconomySqlSchema))
        return runtime
    }

    private fun service(
        runtime: HikariDatabaseRuntime,
        starting: String = "0.00",
        maximum: String = "1000.00",
    ): EconomyService {
        val settings = EconomySettings(
            enabled = true,
            startingBalance = BigDecimal(starting),
            maximumBalance = BigDecimal(maximum),
            minimumPayment = BigDecimal("0.01"),
            maximumPayment = BigDecimal(maximum),
        )
        var tick = 0L
        return EconomyService(
            JdbcEconomyRepository(
                runtime,
                "default"
            ),
            { settings }
        ) {
            Instant.ofEpochMilli(++tick)
        }
    }

    private suspend fun EconomyService.amountOf(playerId: UUID): Money =
        (balance(playerId) as BalanceResult.Balance).amount

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
