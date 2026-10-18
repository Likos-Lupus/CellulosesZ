package top.likoslupus.cellulosesz.movement.world

import java.util.*

/** One selectable world: its dimension id and the alias that points at it, when any. */
internal data class WorldEntry(
    val dimension: String,
    val alias: String?,
)

/**
 * Pure alias resolution for `/world`. Aliases are input convenience only; a raw dimension id is
 * always accepted. Resolution is checked against the set of currently loaded dimensions so a typo
 * or an unloaded datapack dimension is reported as unknown rather than silently accepted.
 */
internal class WorldDirectory(aliases: Map<String, String>) {

    private val normalized: Map<String, String> =
        aliases.entries.associate { it.key.lowercase(Locale.ROOT) to it.value }

    /** The dimension id [raw] names, or null when it is neither loaded nor a loaded alias. */
    fun resolve(raw: String, available: Set<String>): String? {
        if (raw in available) {
            return raw
        }
        val target = normalized[raw.lowercase(Locale.ROOT)] ?: return null
        return if (target in available) target else null
    }

    /** The alias displayed for [dimension], or null when only the raw id names it. */
    fun aliasOf(dimension: String): String? =
        normalized.entries.firstOrNull { it.value == dimension }?.key

    /** Every loaded dimension, sorted, paired with its alias. */
    fun entries(available: Set<String>): List<WorldEntry> =
        available.sorted().map { WorldEntry(it, aliasOf(it)) }

    /** Every alias key, for tab completion. */
    fun aliases(): Set<String> =
        normalized.keys

}
