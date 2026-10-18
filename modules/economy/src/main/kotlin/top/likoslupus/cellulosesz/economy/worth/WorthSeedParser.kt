package top.likoslupus.cellulosesz.economy.worth

import top.likoslupus.cellulosesz.economy.money.Money
import java.math.BigDecimal
import java.time.Instant

/**
 * Parses the bundled worth seed. The format is one `item<TAB>price` per line; blank lines and lines
 * starting with `#` are ignored. It is deliberately Minecraft-free and total: a malformed line is
 * skipped rather than aborting the whole import, so a typo can never stop the server from booting.
 */
internal object WorthSeedParser {

    private val ITEM_ID = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

    fun parse(
        text: String,
        at: Instant,
        updatedBy: String? = SEED_AUTHOR,
    ): List<WorthRecord> =
        text.lineSequence()
                .mapNotNull { parseLine(it, at, updatedBy) }
                .toList()

    private fun parseLine(
        line: String,
        at: Instant,
        updatedBy: String?
    ): WorthRecord? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null
        }
        val separator = trimmed
                .indexOf('\t')
                .takeIf { it > 0 }
            ?: return null
        val itemId = trimmed.substring(0, separator).trim()
        val priceText = trimmed.substring(separator + 1).trim()
        if (!ITEM_ID.matches(itemId)) {
            return null
        }
        val price = try {
            Money.of(BigDecimal(priceText))
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        } ?: return null
        if (!price.isPositive) {
            return null
        }
        return WorthRecord(
            itemId = itemId,
            priceMinor = price.toMinorUnitsExact(),
            updatedAt = at,
            updatedBy = updatedBy,
        )
    }

    /** Recorded as `updated_by` for imported rows so the seed is distinguishable from edits. */
    const val SEED_AUTHOR: String = "bundled:default"

}
