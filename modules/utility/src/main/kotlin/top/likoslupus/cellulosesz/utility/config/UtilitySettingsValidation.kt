package top.likoslupus.cellulosesz.utility.config

import top.likoslupus.cellulosesz.foundation.config.ValidationError

/**
 * Utility-owned configuration rules. The application aggregates these errors and attaches them to
 * the root schema path instead of re-implementing feature validation.
 */
public object UtilitySettingsValidation {

    public fun validate(settings: UtilitySettings): List<ValidationError> =
        buildList {
            val kits = settings.kits
            if (kits.maxKits !in 1..10_000) {
                add(
                    ValidationError(
                        "utility.kits.maxKits",
                        "must be in 1..10000",
                    )
                )
            }
            if (kits.maxItemsPerKit !in 1..1_000) {
                add(
                    ValidationError(
                        "utility.kits.maxItemsPerKit",
                        "must be in 1..1000",
                    )
                )
            }
            kits.maxCooldownSeconds?.let { seconds ->
                if (seconds <= 0L) {
                    add(
                        ValidationError(
                            "utility.kits.maxCooldownSeconds",
                            "must be > 0 when set",
                        )
                    )
                }
            }

            val editing = settings.itemEditing
            if (editing.maxItemNameLength !in 1..256) {
                add(
                    ValidationError(
                        "utility.itemEditing.maxItemNameLength",
                        "must be in 1..256",
                    )
                )
            }
            if (editing.maxLoreLines !in 1..100) {
                add(
                    ValidationError(
                        "utility.itemEditing.maxLoreLines",
                        "must be in 1..100",
                    )
                )
            }
            if (editing.maxLoreLineLength !in 1..256) {
                add(
                    ValidationError(
                        "utility.itemEditing.maxLoreLineLength",
                        "must be in 1..256",
                    )
                )
            }
            if (editing.clearConfirmationSeconds !in 1..300) {
                add(
                    ValidationError(
                        "utility.itemEditing.clearConfirmationSeconds",
                        "must be in 1..300",
                    )
                )
            }

            val give = settings.give
            if (give.defaultAmount !in 1..give.maxAmount) {
                add(
                    ValidationError(
                        "utility.give.defaultAmount",
                        "must be in 1..maxAmount",
                    )
                )
            }
            if (give.maxAmount !in 1..MAX_GIVE_AMOUNT) {
                add(
                    ValidationError(
                        "utility.give.maxAmount",
                        "must be in 1..$MAX_GIVE_AMOUNT",
                    )
                )
            }

            val powertools = settings.powertools
            if (powertools.throttleSeconds !in 0..60) {
                add(
                    ValidationError(
                        "utility.powertools.throttleSeconds",
                        "must be in 0..60",
                    )
                )
            }
        }

    private const val MAX_GIVE_AMOUNT: Int = 100_000

}
