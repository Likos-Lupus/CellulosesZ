package top.likoslupus.cellulosesz.economy.sale

import top.likoslupus.cellulosesz.economy.config.SellMultiplier
import java.math.BigDecimal
import java.math.RoundingMode

/** What to do with a candidate the player may not sell: refuse the whole sale, or skip that slot. */
internal enum class SkipPolicy {

    REFUSE,
    SKIP,

}

/**
 * One inventory slot offered to the planner. [unitPriceMinor] is null when the item has no worth;
 * [count] is the number of items in that slot. The planner never sees a Minecraft type.
 */
internal data class SellCandidate(
    val slot: Int,
    val itemId: String,
    val count: Int,
    val unitPriceMinor: Long?,
    val customNamed: Boolean,
)

/** One exact stack the sale removes. [count] may be less than the slot's total for a partial sale. */
internal data class SellLine(
    val slot: Int,
    val itemId: String,
    val count: Int,
    val unitPriceMinor: Long,
)

/** The outcome of planning a sale. Every branch is a normal business outcome. */
internal sealed interface SellPlanResult {

    data class Planned(
        val lines: List<SellLine>,
        val totalMinor: Long,
        val itemCount: Int,
    ) : SellPlanResult

    /** Nothing matched, or everything eligible was skipped by policy. */
    data object Nothing : SellPlanResult

    /** An explicitly targeted item has no worth and the policy refuses instead of skipping. */
    data class Unpriced(val itemId: String) : SellPlanResult

    /** An explicitly targeted stack carries a custom name and naming is not allowed. */
    data class NamedNotAllowed(val itemId: String) : SellPlanResult

    /** The multiplied total is zero or does not fit the configured ceiling. */
    data object ZeroValue : SellPlanResult

    data object ExceedsCap : SellPlanResult

}

/**
 * The pure money math of `/sell`. It computes the exact slot/stack set and the credit total from
 * already-resolved prices and multiplier; storage, permissions and inventories stay outside so the
 * plan is fully unit-testable. The multiplier is applied once to the summed total (never per slot),
 * and the result is truncated toward zero to the exact minor unit — a `Double` is never involved.
 */
internal object SellPlanner {

    fun plan(
        candidates: List<SellCandidate>,
        namedPolicy: SkipPolicy,
        unpricedPolicy: SkipPolicy,
        multiplier: BigDecimal,
        maximumMinor: Long,
        limit: Int? = null,
    ): SellPlanResult {
        val lines = mutableListOf<SellLine>()
        var sum = BigDecimal.ZERO
        var remaining = limit ?: Int.MAX_VALUE

        candidates.forEach { candidate ->
            if (remaining <= 0) return@forEach

            val named = candidate.customNamed
            val unpriced = candidate.unitPriceMinor == null
            if (named && namedPolicy == SkipPolicy.REFUSE) {
                return SellPlanResult.NamedNotAllowed(candidate.itemId)
            }
            if (unpriced && unpricedPolicy == SkipPolicy.REFUSE) {
                return SellPlanResult.Unpriced(candidate.itemId)
            }
            if (named || unpriced || candidate.count <= 0) {
                return@forEach
            }

            val price = candidate.unitPriceMinor
            val take = minOf(candidate.count, remaining)
            lines += SellLine(
                candidate.slot,
                candidate.itemId,
                take,
                price
            )
            sum = sum.add(
                BigDecimal(price)
                        .multiply(BigDecimal(take))
            )
            remaining -= take
        }

        if (lines.isEmpty()) {
            return SellPlanResult.Nothing
        }

        val total = try {
            sum.multiply(multiplier)
                    .setScale(0, RoundingMode.DOWN)
                    .longValueExact()
        } catch (_: ArithmeticException) {
            return SellPlanResult.ExceedsCap
        }
        return when {
            total <= 0L -> SellPlanResult.ZeroValue
            total > maximumMinor -> SellPlanResult.ExceedsCap
            else -> SellPlanResult.Planned(
                lines,
                total,
                lines.sumOf { it.count }
            )
        }
    }

}

/**
 * Resolves the effective sell multiplier. Among the configured grants the seller actually holds,
 * the highest factor wins; without a match the price is paid at face value. Kept pure so it can be
 * tested without a permission backend.
 */
internal object SellMultipliers {

    fun resolve(
        configured: List<SellMultiplier>,
        allowed: (String) -> Boolean,
    ): BigDecimal =
        configured.asSequence()
                .filter { it.permission.isNotBlank() && allowed(it.permission) }
                .map { it.multiplier }
                .maxOrNull()
            ?: BigDecimal.ONE

}
