package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.economy.account.EconomyAccountId
import top.likoslupus.cellulosesz.economy.ledger.*
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.nio.file.Path
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking

/**
 * A transient backend failure (deadlock / serialization conflict / lock timeout) is retried a bounded
 * number of times; a recognized failure that never clears is surfaced as a typed error rather than a
 * false success, and an unrecognized failure is never retried.
 */
class EconomyRetryTest {

    @TempDir
    lateinit var directory: Path

    private val maximum = Money.parse("1000.00")

    /** A [DatabaseRuntime] that fails its first [failures] transactions before delegating. */
    private class FlakyRuntime(
        private val delegate: DatabaseRuntime,
        private val failures: Int,
        private val failure: SQLException,
    ) : DatabaseRuntime {

        var attempts: Int = 0
            private set

        override val type get() = delegate.type
        override val identity get() = delegate.identity

        override fun <T> read(block: (Connection) -> T): T = delegate.read(block)

        override fun <T> transaction(block: (Connection) -> T): T {
            attempts++
            if (attempts <= failures) {
                throw failure
            }
            return delegate.transaction(block)
        }

        override fun close() = delegate.close()

    }

    private fun sqlite(): HikariDatabaseRuntime {
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

    private fun header(key: String): LedgerHeader = LedgerHeader(
        kind = TransactionKind.TRANSFER,
        originId = "test",
        requestKey = key,
        requestHash = "hash-$key",
        actorKind = ActorKind.INTERNAL,
        actorId = "test",
        reasonCode = "test",
        referenceId = null,
        createdAt = Instant.ofEpochMilli(1),
    )

    private fun lines(alice: UUID, bob: UUID): List<LedgerLine> = listOf(
        LedgerLine(
            EconomyAccountId.Player(alice),
            Money.parse("25.00"),
            LedgerLineRole.DEBIT_PLAYER
        ),
        LedgerLine(
            EconomyAccountId.Player(bob),
            Money.parse("25.00"),
            LedgerLineRole.CREDIT_PLAYER
        ),
    )

    private suspend fun openTwo(runtime: DatabaseRuntime): Pair<UUID, UUID> {
        val repository = JdbcEconomyRepository(runtime, "default")
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()
        repository.ensurePlayerAccount(
            alice,
            Money.parse("100.00"),
            maximum,
            Instant.ofEpochMilli(1)
        )
        repository.ensurePlayerAccount(
            bob,
            Money.parse("0.00"),
            maximum,
            Instant.ofEpochMilli(1)
        )
        return alice to bob
    }

    private suspend fun balanceOf(repository: EconomyRepository, playerId: UUID): Money =
        repository.account(EconomyAccountId.Player(playerId))!!.balance

    @Test
    fun `classification accepts deadlock, serialization, lock timeout and sqlite busy`() {
        assertTrue(isTransientBackendFailure(SQLException("serialization", "40001", 0)))
        assertTrue(isTransientBackendFailure(SQLException("deadlock", "40P01", 0)))
        assertTrue(isTransientBackendFailure(SQLException("mysql deadlock", null, 1213)))
        assertTrue(isTransientBackendFailure(SQLException("mysql lock wait", null, 1205)))
        assertTrue(isTransientBackendFailure(SQLException("h2 lock timeout", null, 50200)))
        assertTrue(isTransientBackendFailure(SQLException("sqlite busy", null, 5)))
        assertTrue(isTransientBackendFailure(SQLException("sqlite busy recovery", null, 261)))
        assertFalse(isTransientBackendFailure(SQLException("boom", null, 12345)))
        assertFalse(isTransientBackendFailure(SQLException("unique", "23505", 0)))
    }

    @Test
    fun `a transient lock failure is retried and commits exactly once`() =
        runBlocking {
            sqlite().use { real ->
                val (alice, bob) = openTwo(real)
                val flaky = FlakyRuntime(
                    real,
                    failures = 1,
                    failure = SQLException("deadlock", "40001", 0)
                )
                val repository = JdbcEconomyRepository(
                    flaky,
                    "default"
                )

                val result = repository.commit(
                    header("t1"),
                    lines(alice, bob),
                    maximum
                )

                assertInstanceOf(
                    LedgerCommitResult.Committed::class.java,
                    result
                )
                assertEquals(2, flaky.attempts)
                assertEquals(
                    Money.parse("75.00"),
                    balanceOf(repository, alice)
                )
                assertEquals(
                    Money.parse("25.00"),
                    balanceOf(repository, bob)
                )
            }
        }

    @Test
    fun `a transient failure that never clears is surfaced, never a false success`() =
        runBlocking {
            sqlite().use { real ->
                val (alice, bob) = openTwo(real)
                val flaky = FlakyRuntime(
                    real,
                    failures = 5,
                    failure = SQLException("deadlock", "40001", 0)
                )
                val repository = JdbcEconomyRepository(
                    flaky,
                    "default"
                )

                val failure = runCatching {
                    repository.commit(
                        header("t1"),
                        lines(alice, bob),
                        maximum
                    )
                }.exceptionOrNull()

                assertInstanceOf(
                    EconomyDataException::class.java,
                    failure
                )
                assertEquals(3, flaky.attempts)
                assertEquals(
                    Money.parse("100.00"),
                    balanceOf(repository, alice)
                )
                assertEquals(
                    Money.parse("0.00"),
                    balanceOf(repository, bob)
                )
            }
        }

    @Test
    fun `an unrecognized failure is not retried`() =
        runBlocking {
            sqlite().use { real ->
                val (alice, bob) = openTwo(real)
                val flaky = FlakyRuntime(
                    real,
                    failures = 5,
                    failure = SQLException("boom", null, 12345)
                )
                val repository = JdbcEconomyRepository(
                    flaky,
                    "default"
                )

                val failure = runCatching {
                    repository.commit(
                        header("t1"),
                        lines(alice, bob),
                        maximum
                    )
                }.exceptionOrNull()

                assertInstanceOf(
                    EconomyDataException::class.java,
                    failure
                )
                assertEquals(1, flaky.attempts)
            }
        }

}
