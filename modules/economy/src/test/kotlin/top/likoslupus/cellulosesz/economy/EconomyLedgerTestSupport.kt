package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.economy.config.EconomySettings
import top.likoslupus.cellulosesz.economy.ledger.ActorKind
import top.likoslupus.cellulosesz.economy.ledger.JdbcEconomyRepository
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.service.BalanceResult
import top.likoslupus.cellulosesz.economy.service.EconomyActor
import top.likoslupus.cellulosesz.economy.service.EconomyService
import top.likoslupus.cellulosesz.foundation.database.DatabaseDescriptorResolver
import top.likoslupus.cellulosesz.foundation.database.DatabaseSettings
import top.likoslupus.cellulosesz.foundation.database.HikariDatabaseRuntime
import top.likoslupus.cellulosesz.foundation.database.SqliteSettings
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Instant
import java.util.*

/**
 * The SQLite ledger fixture shared by [EconomyLedgerCommitTest] and [EconomyLedgerQueryTest]:
 * one fresh database per test, the same `default` namespace, and a deterministic clock.
 */
internal abstract class EconomyLedgerTestSupport {

    @TempDir
    lateinit var directory: Path

    protected val actor = EconomyActor(ActorKind.INTERNAL, "test")

    protected fun runtime(): HikariDatabaseRuntime {
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

    protected fun service(
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

    protected suspend fun EconomyService.amountOf(playerId: UUID): Money =
        (balance(playerId) as BalanceResult.Balance).amount

}
