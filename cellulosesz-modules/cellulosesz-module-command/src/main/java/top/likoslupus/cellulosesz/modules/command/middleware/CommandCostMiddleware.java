package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewarePhase;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.command.service.CommandCostReservation;
import top.likoslupus.cellulosesz.api.command.service.CommandCostReserveResult;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public final class CommandCostMiddleware implements CommandMiddleware {

    private final CommandCostService costs;

    public CommandCostMiddleware(CommandCostService costs) {
        this.costs = costs;
    }

    @Override
    public CommandMiddlewarePhase phase() {
        return CommandMiddlewarePhase.TRANSACTION;
    }

    @Override
    public CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    ) {
        var cost = costs.cost(descriptor.canonicalName());
        if (cost.signum() <= 0 || context.playerUuid().isEmpty()) {
            return continuation.proceed();
        }

        return costs
                .reserve(
                        context.playerUuid().orElseThrow(),
                        descriptor.canonicalName()
                )
                .thenCompose(result -> {
                    if (result instanceof CommandCostReserveResult.Rejected rejected) {
                        context.error(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMON_COMMAND_COST_FAILED,
                                Map.of("cost", rejected.amount().toPlainString())
                        ));
                        return CompletableFuture.completedFuture(CommandOutcome.rejected());
                    }

                    var reservation = ((CommandCostReserveResult.Reserved) result).reservation();
                    final CompletionStage<CommandOutcome> terminal;
                    try {
                        terminal = continuation.proceed();
                    } catch (RuntimeException failure) {
                        return refundAfterFailure(reservation, failure);
                    }

                    return terminal
                            .handle(TerminalResult::new)
                            .thenCompose(terminalResult -> settle(reservation, terminalResult));
                });
    }

    private CompletionStage<CommandOutcome> refundAfterFailure(
            CommandCostReservation reservation,
            Throwable terminalFailure
    ) {
        var original = unwrap(terminalFailure);
        return reservation.refund()
                .handle((_, refundFailure) -> {
                    if (refundFailure != null) {
                        original.addSuppressed(unwrap(refundFailure));
                    }
                    throw new CompletionException(original);
                });
    }

    private CompletionStage<CommandOutcome> settle(
            CommandCostReservation reservation,
            TerminalResult terminal
    ) {
        if (terminal.failure() != null) {
            return refundAfterFailure(reservation, terminal.failure());
        }

        var outcome = terminal.outcome();
        if (outcome == null) {
            return refundAfterFailure(
                    reservation,
                    new IllegalStateException("Command terminal completed without an outcome")
            );
        }

        if (outcome.successful()) {
            return reservation.commit().thenApply(_ -> outcome);
        }

        return reservation.refund()
                .handle((_, refundFailure) -> {
                    if (refundFailure != null) {
                        throw new CompletionException(unwrap(refundFailure));
                    }
                    return outcome;
                });
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while (current instanceof CompletionException
                && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

    private record TerminalResult(
            CommandOutcome outcome,
            Throwable failure
    ) {

    }

}
