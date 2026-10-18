package top.likoslupus.cellulosesz.economy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import top.likoslupus.cellulosesz.economy.ledger.ActorKind
import top.likoslupus.cellulosesz.economy.ledger.JdbcEconomyRepository
import top.likoslupus.cellulosesz.economy.money.Money
import top.likoslupus.cellulosesz.economy.sale.JdbcSaleIntentRepository
import top.likoslupus.cellulosesz.economy.sale.SaleIntentRecord
import top.likoslupus.cellulosesz.economy.sale.SaleIntentState
import top.likoslupus.cellulosesz.economy.service.EconomyActor
import top.likoslupus.cellulosesz.economy.service.EconomyService
import top.likoslupus.cellulosesz.economy.service.GrantIntent
import top.likoslupus.cellulosesz.foundation.database.*
import top.likoslupus.cellulosesz.foundation.database.schema.SchemaInitializer
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlinx.coroutines.runBlocking

/** `cz_economy_sale_intents`, its guarded transitions and the ledger credit lookup. */
class SaleIntentBackendsTest {

    @TempDir
    lateinit var directory: Path

    private fun runtime(type: DatabaseType): HikariDatabaseRuntime {
        val descriptor = when (type) {
            DatabaseType.H2 -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    type = DatabaseType.H2,
                    h2 = H2Settings(path = directory.resolve("sale_h2").toString()),
                ),
                directory,
            )

            else -> DatabaseDescriptorResolver.resolve(
                DatabaseSettings(
                    sqlite = SqliteSettings(
                        path = directory.resolve("sale_sqlite.db").toString()
                    )
                ),
                directory,
            )
        }
        val runtime = HikariDatabaseRuntime.create(descriptor)
        SchemaInitializer.initializeBlocking(runtime, listOf(EconomySqlSchema))
        return runtime
    }

    private fun record(
        saleId: UUID = UUID.randomUUID(),
        state: SaleIntentState = SaleIntentState.PREPARED,
        name: String = "Alex",
    ) = SaleIntentRecord(
        saleId = saleId,
        playerId = UUID.randomUUID(),
        playerName = name,
        state = state,
        itemId = "minecraft:diamond",
        itemCount = 3,
        totalMinor = 15000L,
        payload = "[]",
        transactionId = null,
        createdAtMs = 1L,
        updatedAtMs = 1L,
    )

    @Test
    fun `transitions are guarded and loadOpen excludes terminal rows on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val repository = JdbcSaleIntentRepository(runtime, "default")
                    val saleId = UUID.randomUUID()
                    repository.insert(record(saleId))
                    assertEquals(
                        listOf(saleId),
                        repository.loadOpen().map { it.saleId }
                    )

                    // COMMITTED cannot be reached before TAKEN.
                    assertFalse(
                        repository.markCommitted(
                            saleId,
                            UUID.randomUUID(),
                            Instant.EPOCH
                        )
                    )
                    assertTrue(repository.markTaken(saleId, Instant.EPOCH))

                    val transactionId = UUID.randomUUID()
                    assertTrue(
                        repository.markCommitted(
                            saleId,
                            transactionId,
                            Instant.EPOCH
                        )
                    )
                    assertTrue(repository.loadOpen().isEmpty())

                    val stored = repository.loadOpen()
                    assertTrue(stored.isEmpty())
                }
            }
        }

    @Test
    fun `rolled back is terminal and idempotent on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val repository = JdbcSaleIntentRepository(runtime, "default")
                    val prepared = UUID.randomUUID()
                    repository.insert(record(prepared))

                    assertTrue(repository.markRolledBack(prepared, Instant.EPOCH))
                    assertFalse(repository.markRolledBack(prepared, Instant.EPOCH))
                    assertFalse(repository.markTaken(prepared, Instant.EPOCH))
                    assertTrue(repository.loadOpen().isEmpty())
                }
            }
        }

    @Test
    fun `recovery finds the sale credit only when the ledger recorded it on every backend`() =
        backends().forEach { type ->
            runBlocking {
                runtime(type).use { runtime ->
                    val ledger = JdbcEconomyRepository(runtime, "default")
                    val service = EconomyService(
                        ledger,
                        {
                            top.likoslupus.cellulosesz.economy.config.EconomySettings(
                                enabled = true,
                                maximumBalance = BigDecimal("1000000.00"),
                            )
                        },
                    ) { Instant.EPOCH }
                    val intents = JdbcSaleIntentRepository(runtime, "default")
                    val player = UUID.randomUUID()
                    val saleId = UUID.randomUUID()

                    service.grant(
                        GrantIntent(
                            EconomyActor(ActorKind.PLAYER, player.toString()),
                            player,
                            Money.parse("150.00"),
                            "sell",
                            saleId.toString(),
                            "item.sell",
                        )
                    )

                    assertNotNull(
                        ledger.transactionId(
                            "sell",
                            saleId.toString()
                        )
                    )
                    assertNull(
                        ledger.transactionId(
                            "sell",
                            UUID.randomUUID().toString()
                        )
                    )
                    // A `TAKEN` row for the committed sale must not be restored.
                    intents.insert(record(saleId, SaleIntentState.TAKEN))
                    assertEquals(
                        listOf(SaleIntentState.TAKEN),
                        intents.loadOpen().map { it.state },
                    )
                }
            }
        }

    private fun backends(): List<DatabaseType> =
        listOf(
            DatabaseType.SQLITE,
            DatabaseType.H2,
        )

}
