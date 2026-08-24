package top.likoslupus.cellulosesz.modules.economy.application;

import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.math.BigDecimal;

import static top.likoslupus.cellulosesz.api.validation.Checks.*;

import static java.util.Objects.requireNonNull;

/**
 * Immutable, fully validated command-facing economy configuration.
 */
public record EconomyCommandSettings(
        int scale,
        BigDecimal minimumBalance,
        BigDecimal maximumBalance,
        BigDecimal minimumPayment,
        BigDecimal confirmationThreshold,
        boolean respectIgnore,
        boolean allowOfflineByDefault,
        int maximumRecipients,
        int balanceTopPageSize,
        long version
) {

    public EconomyCommandSettings {
        requireNonNull(minimumBalance, "minimumBalance");
        requireNonNull(maximumBalance, "maximumBalance");
        requireNonNull(minimumPayment, "minimumPayment");
        requireNonNull(confirmationThreshold, "confirmationThreshold");
        requireInRange(scale, 0, 18, "currency scale");
        if (minimumBalance.compareTo(maximumBalance) > 0) {
            throw new IllegalArgumentException("minimum balance exceeds maximum balance");
        }
        requirePositive(minimumPayment, "minimumPayment");
        requireNonNegative(confirmationThreshold, "confirmationThreshold");
        requirePositive(maximumRecipients, "maximumRecipients");
        requirePositive(balanceTopPageSize, "balanceTopPageSize");
    }

    public static EconomyCommandSettings from(EconomyConfig config, long version) {
        requireNonNull(config, "config");
        return new EconomyCommandSettings(
                config.currency.scale,
                decimal(config.minimumBalance, "minimumBalance"),
                decimal(config.maximumBalance, "maximumBalance"),
                decimal(config.pay.minimum, "pay.minimum"),
                decimal(config.pay.requireConfirmAbove, "pay.requireConfirmAbove"),
                config.pay.respectIgnore,
                config.pay.allowOfflineByDefault,
                config.pay.maximumRecipients,
                config.balanceTop.pageSize,
                version
        );
    }

    private static BigDecimal decimal(String value, String name) {
        try {
            return new BigDecimal(requireNonNull(value, name).trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(name + " must be a decimal", failure);
        }
    }

}
