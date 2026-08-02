package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.command.service.CommandCostReservation;
import top.likoslupus.cellulosesz.api.command.service.CommandCostReserveResult;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Objects.requireNonNull;

public final class DefaultCommandCostService implements CommandCostService {

    private final ServiceRegistry services;
    private volatile Map<String, BigDecimal> costs = Map.of();

    public DefaultCommandCostService(ServiceRegistry services) {
        this.services = requireNonNull(services, "services");
    }

    public void configure(Map<String, BigDecimal> configured) {
        var next = new LinkedHashMap<String, BigDecimal>();
        configured.forEach((key, value) -> {
            requireNonNull(value, "command cost");
            if (value.signum() > 0) {
                next.put(normalize(key), value);
            }
        });
        costs = Map.copyOf(next);
    }

    private String normalize(String command) {
        return command.trim().toLowerCase(Locale.ROOT);
    }

    public Map<String, BigDecimal> snapshot() {
        return costs;
    }

    private static final class NoopReservation implements CommandCostReservation {

        private final CompletableFuture<Void> terminal = new CompletableFuture<>();
        private State state = State.RESERVED;

        @Override
        public BigDecimal amount() {
            return BigDecimal.ZERO;
        }

        @Override
        public synchronized CompletionStage<Void> commit() {
            if (state == State.COMMITTED) {
                return terminal;
            }

            if (state != State.RESERVED) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "No-op command cost reservation cannot be committed after " + state
                ));
            }

            state = State.COMMITTED;
            terminal.complete(null);
            return terminal;
        }

        @Override
        public synchronized CompletionStage<Void> refund() {
            if (state == State.REFUNDED) {
                return terminal;
            }

            if (state != State.RESERVED) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "No-op command cost reservation cannot be refunded after " + state
                ));
            }

            state = State.REFUNDED;
            terminal.complete(null);
            return terminal;
        }

        private enum State {

            RESERVED,
            COMMITTED,
            REFUNDED

        }

    }

    private static final class EconomyReservation implements CommandCostReservation {

        private final EconomyService economy;
        private final UUID uuid;
        private final String command;
        private final BigDecimal amount;
        private final TransactionCause cause;
        private State state = State.RESERVED;
        private CompletableFuture<Void> terminal = new CompletableFuture<>();

        private EconomyReservation(
                EconomyService economy,
                UUID uuid,
                String command,
                BigDecimal amount,
                TransactionCause cause
        ) {
            this.economy = economy;
            this.uuid = uuid;
            this.command = command;
            this.amount = amount;
            this.cause = cause;
        }

        @Override
        public BigDecimal amount() {
            return amount;
        }

        @Override
        public synchronized CompletionStage<Void> commit() {
            if (state == State.COMMITTED) {
                return terminal;
            }

            if (state != State.RESERVED) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Command cost reservation cannot be committed after " + state
                ));
            }

            state = State.COMMITTED;
            terminal.complete(null);
            return terminal;
        }

        @Override
        public synchronized CompletionStage<Void> refund() {
            if (state == State.REFUNDED
                    || state == State.REFUNDING
                    || state == State.REFUND_FAILED
            ) {
                return terminal;
            }

            if (state != State.RESERVED) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Command cost reservation cannot be refunded after " + state
                ));
            }

            state = State.REFUNDING;
            var refundFuture = new CompletableFuture<Void>();
            terminal = refundFuture;
            final CompletionStage<top.likoslupus.cellulosesz.api.economy.TransactionResult> refund;
            try {
                refund = economy.deposit(
                        uuid,
                        amount,
                        TransactionCause.command(
                                cause.actor(),
                                command + " refund"
                        )
                );
            } catch (RuntimeException failure) {
                state = State.REFUND_FAILED;
                refundFuture.completeExceptionally(failure);
                return refundFuture;
            }

            refund.whenComplete((result, failure) -> {
                synchronized (EconomyReservation.this) {
                    if (failure != null) {
                        state = State.REFUND_FAILED;
                        refundFuture.completeExceptionally(failure);
                    } else if (!result.success()) {
                        state = State.REFUND_FAILED;
                        refundFuture.completeExceptionally(new IllegalStateException(
                                "Economy rejected command cost refund for /" + command
                        ));
                    } else {
                        state = State.REFUNDED;
                        refundFuture.complete(null);
                    }
                }
            });

            return refundFuture;
        }

        private enum State {

            RESERVED,
            COMMITTED,
            REFUNDING,
            REFUNDED,
            REFUND_FAILED

        }

    }

    @Override
    public BigDecimal cost(String command) {
        return costs.getOrDefault(normalize(command), BigDecimal.ZERO);
    }

    @Override
    public CompletionStage<CommandCostReserveResult> reserve(UUID uuid, String command) {
        requireNonNull(uuid, "uuid");

        var canonicalCommand = normalize(command);
        var amount = cost(canonicalCommand);

        if (amount.signum() <= 0) {
            return CompletableFuture.completedFuture(new CommandCostReserveResult.Reserved(
                    new NoopReservation()
            ));
        }

        var economy = services.optional(EconomyService.class);
        if (economy.isEmpty()) {
            return CompletableFuture.completedFuture(new CommandCostReserveResult.Rejected(
                    amount,
                    CommandCostReserveResult.Reason.ECONOMY_UNAVAILABLE
            ));
        }

        var cause = TransactionCause.command("cellulosesz", canonicalCommand);
        return economy.orElseThrow()
                .withdraw(uuid, amount, cause)
                .thenApply(result -> result.success()
                        ?
                        new CommandCostReserveResult.Reserved(
                                new EconomyReservation(
                                        economy.orElseThrow(),
                                        uuid,
                                        canonicalCommand,
                                        amount,
                                        cause
                                )
                        )
                        : new CommandCostReserveResult.Rejected(
                                amount,
                                CommandCostReserveResult.Reason.RESERVATION_DECLINED
                        )
                );
    }

}
