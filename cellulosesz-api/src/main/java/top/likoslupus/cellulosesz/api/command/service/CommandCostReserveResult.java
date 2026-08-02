package top.likoslupus.cellulosesz.api.command.service;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

public sealed interface CommandCostReserveResult
        permits CommandCostReserveResult.Reserved, CommandCostReserveResult.Rejected {

    BigDecimal amount();

    enum Reason {
        ECONOMY_UNAVAILABLE,
        RESERVATION_DECLINED
    }

    record Reserved(CommandCostReservation reservation) implements CommandCostReserveResult {

        public Reserved {
            requireNonNull(reservation, "reservation");
        }

        @Override
        public BigDecimal amount() {
            return reservation.amount();
        }

    }

    record Rejected(
            BigDecimal amount,
            Reason reason
    ) implements CommandCostReserveResult {

        public Rejected {
            requireNonNull(amount, "amount");
            requireNonNull(reason, "reason");
        }

    }

}
