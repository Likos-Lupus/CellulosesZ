package top.likoslupus.cellulosesz.utility.itemtools

import java.util.*

/** Pure policy for `/give` and `/item` item ids, blacklist matching and amount bounds. */
internal object ItemToolsPolicy {

    /** Lowercases an id and defaults a missing namespace to `minecraft:`. */
    fun normalizeId(raw: String): String {
        val trimmed = raw.trim().lowercase(Locale.ROOT)
        return when {
            trimmed.isEmpty() -> trimmed
            trimmed.contains(':') -> trimmed
            else -> "minecraft:$trimmed"
        }
    }

    fun isBlacklisted(itemId: String, blacklist: List<String>): Boolean {
        val normalized = normalizeId(itemId)
        return blacklist.any { normalizeId(it) == normalized }
    }

    /**
     * Resolves the requested amount against the configured default and maximum. Returns null when
     * an explicit amount is out of the 1..[max] range.
     */
    fun resolveAmount(requested: Int?, default: Int, max: Int): Int? =
        when (requested) {
            null -> default
            else -> requested.takeIf { it in 1..max }
        }

}
