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
        }

}
