package top.likoslupus.cellulosesz.application.api.extension

import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.api.extension.KitClaimContextView
import top.likoslupus.cellulosesz.api.extension.KitEligibilityDecision
import top.likoslupus.cellulosesz.utility.kit.KitClaimEligibility
import top.likoslupus.cellulosesz.utility.kit.KitEligibilityDecision as UtilityDecision
import java.util.UUID

/**
 * Composes every registered addon kit-eligibility checker into the single [KitClaimEligibility] the
 * utility feature receives. It is fail-closed: a checker exception denies the claim. It can only add
 * a denial — it never bypasses the kit's own Once/Cooldown/inventory/reservation rules.
 */
internal class ComposedKitEligibility(
    private val extensions: ApplicationExtensionHub,
) : KitClaimEligibility {

    override fun evaluate(playerId: UUID, kitName: String): UtilityDecision {
        val checkers = extensions.kitEligibilities()
        if (checkers.isEmpty()) {
            return UtilityDecision.Allow
        }
        val view = KitClaimContextView(playerId, kitName)
        checkers.forEach { checker ->
            val result = try {
                checker.check(view)
            } catch (failure: Throwable) {
                LOGGER.warn(
                    "kit_eligibility_failed player={} kit={} type={}",
                    playerId,
                    kitName,
                    failure.javaClass.simpleName,
                    failure,
                )
                return UtilityDecision.Deny("kit.eligibility.error")
            }
            when (result) {
                KitEligibilityDecision.Allow -> Unit
                is KitEligibilityDecision.Deny -> return UtilityDecision.Deny(result.reasonKey)
            }
        }
        return UtilityDecision.Allow
    }

    private companion object {

        private val LOGGER = LoggerFactory.getLogger(ComposedKitEligibility::class.java)

    }

}
