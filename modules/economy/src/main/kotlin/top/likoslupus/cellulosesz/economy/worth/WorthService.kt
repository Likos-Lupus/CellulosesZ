package top.likoslupus.cellulosesz.economy.worth

import top.likoslupus.cellulosesz.economy.ledger.EconomyDataException
import top.likoslupus.cellulosesz.economy.money.Money
import java.time.Instant

/**
 * The worth catalog's business entry. It hydrates the in-memory cache from storage exactly once at
 * server start and keeps the cache in step with `/setworth`; it never blocks the server thread on
 * JDBC and never writes a price without persisting it first.
 */
internal class WorthService(
    private val repository: WorthRepository,
    private val cache: WorthCache,
    private val seedEnabled: () -> Boolean,
    private val now: () -> Instant,
    private val seedText: suspend () -> String?,
) {

    /**
     * Loads every stored price; when the table is empty and seeding is enabled, imports the bundled
     * default catalog once. Returns false when storage is unavailable, leaving the cache as-is.
     */
    suspend fun hydrate(): Boolean =
        try {
            val stored = repository.loadAll()
            if (stored.isEmpty() && seedEnabled()) {
                val text = seedText()
                if (!text.isNullOrBlank()) {
                    repository.insertMissing(WorthSeedParser.parse(text, now()))
                    cache.replaceAll(repository.loadAll().map { it.toEntry() })
                    return true
                }
            }
            cache.replaceAll(stored.map { it.toEntry() })
            true
        } catch (_: EconomyDataException) {
            false
        }

    /** Persists [price] for [itemId] and mirrors it into the cache. Returns false on storage failure. */
    suspend fun set(
        itemId: String,
        price: Money,
        updatedBy: String?
    ): Boolean =
        try {
            repository.upsert(
                WorthRecord(
                    itemId = itemId,
                    priceMinor = price.toMinorUnitsExact(),
                    updatedAt = now(),
                    updatedBy = updatedBy,
                )
            )
            cache.put(itemId, price)
            true
        } catch (_: EconomyDataException) {
            false
        }

    fun unitPrice(itemId: String): Money? =
        cache.unitPrice(itemId)

    fun entries(): List<WorthEntry> =
        cache.snapshot()

    private fun WorthRecord.toEntry(): WorthEntry =
        WorthEntry(itemId, Money.fromMinorUnits(priceMinor))

}
