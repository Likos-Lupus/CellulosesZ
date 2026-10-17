package top.likoslupus.cellulosesz.economy.config

import top.likoslupus.cellulosesz.foundation.config.ValidationError
import java.math.BigDecimal
import java.math.RoundingMode

/** Root-schema rules for the economy section; called by the composition root's config validator. */
public object EconomySettingsValidation {

    private val MAX_MINOR = BigDecimal(Long.MAX_VALUE).movePointLeft(2)

    private const val MAX_LABEL_LENGTH = 32

    public fun validate(settings: EconomySettings): List<ValidationError> =
        buildList {
            validateAmount(
                "economy.starting-balance",
                settings.startingBalance
            ) { value ->
                if (value.signum() < 0) {
                    add(
                        ValidationError(
                            "economy.starting-balance",
                            "starting balance must not be negative"
                        )
                    )
                }
            }
            validateAmount(
                "economy.maximum-balance",
                settings.maximumBalance
            ) { value ->
                when {
                    value.signum() <= 0 ->
                        add(
                            ValidationError(
                                "economy.maximum-balance",
                                "maximum balance must be positive"
                            )
                        )

                    value > MAX_MINOR ->
                        add(
                            ValidationError(
                                "economy.maximum-balance",
                                "maximum balance is too large"
                            )
                        )
                }
            }
            validateAmount(
                "economy.minimum-payment",
                settings.minimumPayment
            ) { value ->
                if (value.signum() <= 0) {
                    add(
                        ValidationError(
                            "economy.minimum-payment",
                            "minimum payment must be positive"
                        )
                    )
                }
            }
            validateAmount(
                "economy.maximum-payment",
                settings.maximumPayment
            ) { value ->
                if (value.signum() <= 0) {
                    add(
                        ValidationError(
                            "economy.maximum-payment",
                            "maximum payment must be positive"
                        )
                    )
                }
            }

            if (settings.startingBalance > settings.maximumBalance) {
                add(
                    ValidationError(
                        "economy.starting-balance",
                        "starting balance exceeds the maximum balance"
                    )
                )
            }
            if (settings.minimumPayment > settings.maximumPayment) {
                add(
                    ValidationError(
                        "economy.minimum-payment",
                        "minimum payment exceeds the maximum payment",
                    )
                )
            }
            if (settings.maximumPayment > settings.maximumBalance) {
                add(
                    ValidationError(
                        "economy.maximum-payment",
                        "maximum payment exceeds the maximum balance"
                    )
                )
            }

            validateLabel(
                "economy.currency-name",
                settings.currencyName
            )
            validateLabel(
                "economy.currency-symbol",
                settings.currencySymbol
            )

            if (settings.historyPageSize !in 1..50) {
                add(
                    ValidationError(
                        "economy.history-page-size",
                        "history page size must be between 1 and 50"
                    )
                )
            }
            if (settings.balanceTopPageSize !in 1..50) {
                add(
                    ValidationError(
                        "economy.balance-top-page-size",
                        "ranking page size must be between 1 and 50"
                    )
                )
            }
        }

    private fun MutableList<ValidationError>.validateAmount(
        field: String,
        value: BigDecimal,
        extra: MutableList<ValidationError>.(BigDecimal) -> Unit,
    ) {
        val scaled = try {
            value.setScale(2, RoundingMode.UNNECESSARY)
        } catch (_: ArithmeticException) {
            add(
                ValidationError(
                    field,
                    "amount must have at most two fractional digits"
                )
            )
            return
        }
        extra(scaled)
    }

    private fun MutableList<ValidationError>.validateLabel(field: String, value: String) {
        when {
            value.isBlank() ->
                add(
                    ValidationError(
                        field,
                        "value must not be blank"
                    )
                )

            value.length > MAX_LABEL_LENGTH ->
                add(
                    ValidationError(
                        field,
                        "value is too long"
                    )
                )

            value.any { it.isISOControl() } ->
                add(
                    ValidationError(
                        field,
                        "value contains control characters"
                    )
                )
        }
    }

}
