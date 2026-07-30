package top.likoslupus.cellulosesz.api.economy;

import java.math.BigDecimal;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record BalanceFilter(
        Optional<BigDecimal> minimum,
        Optional<BigDecimal> maximum
) {

    public BalanceFilter {
        requireNonNull(minimum, "minimum");
        requireNonNull(maximum, "maximum");
        if (minimum.isPresent() && maximum.isPresent()
                && minimum.orElseThrow().compareTo(maximum.orElseThrow()
        ) > 0) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
    }

    public static BalanceFilter all() {
        return new BalanceFilter(Optional.empty(), Optional.empty());
    }

}
