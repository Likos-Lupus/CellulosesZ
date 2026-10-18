package top.likoslupus.cellulosesz.economy.worth

import top.likoslupus.cellulosesz.economy.money.Money
import java.time.Instant

/**
 * One persisted worth row. [priceMinor] is the exact unit price in BIGINT minor units; the item id
 * is a `namespace:path` [net.minecraft.resources.Identifier] string and is the only key.
 */
internal data class WorthRecord(
    val itemId: String,
    val priceMinor: Long,
    val updatedAt: Instant,
    val updatedBy: String?,
)

/** A read-only worth entry as exposed to commands. */
internal data class WorthEntry(
    val itemId: String,
    val price: Money,
)

/**
 * The in-memory worth catalog. It is hydrated once at server start and kept in step with every
 * `/setworth`; the sell path reads it synchronously on the server thread, so it holds no lock and
 * never touches storage.
 */
internal class WorthCache {

    private val prices = HashMap<String, Money>()

    fun replaceAll(entries: List<WorthEntry>) {
        prices.clear()
        entries.forEach { prices[it.itemId] = it.price }
    }

    fun put(itemId: String, price: Money) {
        prices[itemId] = price
    }

    fun unitPrice(itemId: String): Money? =
        prices[itemId]

    fun snapshot(): List<WorthEntry> =
        prices.entries
                .sortedBy { it.key }
                .map { WorthEntry(it.key, it.value) }

    fun size(): Int =
        prices.size

    fun isEmpty(): Boolean =
        prices.isEmpty()

}
