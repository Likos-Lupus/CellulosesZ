package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.ledger.ActorKind
import top.likoslupus.cellulosesz.economy.ledger.JdbcEconomyRepository
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.service.*
import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlinx.coroutines.*

/**
 * The same ledger contract as [EconomyLedgerCommitTest] and [EconomyLedgerQueryTest], exercised
 * against both supported local backends (SQLite and H2), plus a concurrency case that proves the
 * guarded conditional updates never overdraw or lose funds under interleaved requests.
 */
class EconomyLedgerBackendsTest {

    @TempDir
    lateinit var directory: Path

    private val actor = EconomyActor(ActorKind.INTERNAL, "test")

    private fun runtime(type: DatabaseType): HikariDatabaseRuntime {
        val descriptor = when (type) {
            DatabaseType.H2 -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    type = DatabaseType.H2,
                    h2 = H2Settings(path = directory.resolve("cz_h2").toString()),
                ),
                directory,
            )

            else -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    sqlite = SqliteSettings(
                        path = directory.resolve("cz_sqlite.db")
                                .toString()
                    )
                ),
                directory,
            )
        }
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

    private fun backends(): List<DatabaseType> =
        listOf(
            DatabaseType.SQLITE,
            DatabaseType.H2
        )

    @Test
    fun `opening is applied exactly once on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
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
        }

    @Test
    fun `transfer conserves the total and idempotent replay is exact on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
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
                            "k",
                            "pay"
                        )
                    )
                    val replay = service.transfer(
                        TransferIntent(
                            actor,
                            alice,
                            bob,
                            Money.parse("25.00"),
                            "test",
                            "k",
                            "pay"
                        )
                    )

                    assertInstanceOf(
                        EconomyMutationResult.Committed::class.java,
                        first
                    )
                    assertInstanceOf(
                        EconomyMutationResult.AlreadyCommitted::class.java,
                        replay
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
        }

    @Test
    fun `insufficient funds and balance-limit rejections leave balances untouched on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val service = service(runtime, maximum = "100.00")
                    val alice = UUID.randomUUID()
                    val bob = UUID.randomUUID()
                    service.ensureAccount(alice)
                    service.ensureAccount(bob)
                    service.grant(
                        GrantIntent(
                            actor,
                            alice,
                            Money.parse("40.00"),
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

                    val insufficient = service.transfer(
                        TransferIntent(
                            actor,
                            alice,
                            bob,
                            Money.parse("50.00"),
                            "test",
                            "i",
                            "pay"
                        )
                    )
                    assertEquals(EconomyMutationResult.InsufficientFunds, insufficient)

                    val overLimit = service.transfer(
                        TransferIntent(
                            actor,
                            alice,
                            bob,
                            Money.parse("20.00"),
                            "test",
                            "o",
                            "pay"
                        )
                    )
                    assertEquals(
                        EconomyMutationResult.BalanceLimitExceeded,
                        overLimit
                    )

                    assertEquals(
                        Money.parse("40.00"),
                        service.amountOf(alice)
                    )
                    assertEquals(
                        Money.parse("90.00"),
                        service.amountOf(bob)
                    )
                }
            }
        }

    @Test
    fun `a durable no-change set survives a later balance change on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
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

                    assertInstanceOf(
                        EconomyMutationResult.Committed::class.java,
                        service.set(
                            SetIntent(
                                actor,
                                alice,
                                Money.parse("40.00"),
                                "test",
                                "s1",
                                "admin"
                            )
                        ),
                    )
                    assertInstanceOf(
                        EconomyMutationResult.NoChange::class.java,
                        service.set(
                            SetIntent(
                                actor,
                                alice,
                                Money.parse("40.00"),
                                "test",
                                "s2",
                                "admin"
                            )
                        ),
                    )
                    // The player then spends; replaying the recorded no-change key still does nothing.
                    service.grant(
                        GrantIntent(
                            actor,
                            alice,
                            Money.parse("5.00"),
                            "test",
                            "later",
                            "seed"
                        )
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
                    assertInstanceOf(
                        EconomyMutationResult.AlreadyCommitted::class.java,
                        replay
                    )
                    assertEquals(
                        Money.parse("45.00"),
                        service.amountOf(alice)
                    )
                }
            }
        }

    @Test
    fun `concurrent transfers neither overdraw nor lose funds`() =
        runBlocking {
            runtime(DatabaseType.H2).use { runtime ->
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

                val results = coroutineScope {
                    (1..12).map { index ->
                        async(Dispatchers.Default) {
                            runCatching {
                                service.transfer(
                                    TransferIntent(
                                        actor,
                                        alice,
                                        bob,
                                        Money.parse("10.00"),
                                        "test",
                                        "conc:$index",
                                        "pay",
                                    )
                                )
                            }
                        }
                    }.awaitAll()
                }

                // A backend lock conflict may surface as a thrown data exception; either way the
                // ledger must stay exact: no overdraw, no lost funds, and every player hit matches a
                // committed transaction.
                val committed = results.count { it.getOrNull() is EconomyMutationResult.Committed }
                val aliceNow = service.amountOf(alice)
                val bobNow = service.amountOf(bob)

                assertTrue(committed >= 1)
                assertTrue(aliceNow.isNonNegative)
                assertEquals(
                    0,
                    aliceNow
                            .plus(bobNow).decimal
                            .compareTo(BigDecimal("100.00"))
                )
                assertEquals(
                    0,
                    bobNow.decimal.compareTo(
                        BigDecimal(committed)
                                .multiply(BigDecimal("10.00"))
                    ),
                )
            }
        }

}
