package top.likoslupus.cellulosesz.administration.playerstate

/** `/exp` operation, matching the sub-commands `give`, `take` and `set`. */
internal enum class ExperienceOperation {

    GIVE,
    TAKE,
    SET,

}

/**
 * The resolved change for one `/exp` mutation: [applied] is the reported amount and [delta] is what
 * is handed to `Player.giveExperiencePoints` (which clamps the running total at zero).
 */
internal data class ExperienceChange(
    val applied: Int,
    val delta: Int,
)

/** Pure `/exp` arithmetic: clamps every input to `0..maximumAdjustment` and never overshoots zero. */
internal object ExperienceMath {

    fun clampAdjustment(amount: Int, maxAdjustment: Int): Int =
        amount.coerceIn(0, maxAdjustment)

    fun resolve(
        operation: ExperienceOperation,
        amount: Int,
        currentTotal: Int,
        maxAdjustment: Int,
    ): ExperienceChange =
        when (operation) {
            ExperienceOperation.GIVE -> {
                val capped = clampAdjustment(amount, maxAdjustment)
                ExperienceChange(capped, capped)
            }

            ExperienceOperation.TAKE -> {
                val taken = minOf(clampAdjustment(amount, maxAdjustment), currentTotal)
                ExperienceChange(taken, -taken)
            }

            ExperienceOperation.SET -> {
                val target = clampAdjustment(amount, maxAdjustment)
                ExperienceChange(target, target - currentTotal)
            }
        }

}
