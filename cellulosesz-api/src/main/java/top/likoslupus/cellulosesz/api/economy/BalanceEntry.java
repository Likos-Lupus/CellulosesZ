package top.likoslupus.cellulosesz.api.economy;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceEntry(
        UUID uuid,
        BigDecimal balance
) {

}
