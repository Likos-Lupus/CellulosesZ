package top.likoslupus.cellulosesz.utility.unlimited

/** One inventory slot considered by the unlimited top-up planner. */
internal data class UnlimitedSlot(
    val slot: Int,
    val itemId: String,
    val count: Int,
)

/**
 * Pure top-up rule: an unlimited item the player already holds is topped up to [TARGET_COUNT] when
 * its count has fallen to one. Items with a higher count are never reduced.
 */
internal object UnlimitedTopUp {

    const val TARGET_COUNT: Int = 2

    fun plan(
        slots: List<UnlimitedSlot>,
        unlimited: Set<String>,
    ): List<UnlimitedSlot> =
        slots
                .filter { it.count in 1 until TARGET_COUNT && it.itemId in unlimited }
                .map { it.copy(count = TARGET_COUNT) }

}
