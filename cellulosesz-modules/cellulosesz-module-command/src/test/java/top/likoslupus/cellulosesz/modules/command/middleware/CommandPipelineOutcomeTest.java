package top.likoslupus.cellulosesz.modules.command.middleware;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewarePhase;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.command.service.CommandCostReservation;
import top.likoslupus.cellulosesz.api.command.service.CommandCostReserveResult;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.command.execution.DefaultCommandExecutionPipeline;
import top.likoslupus.cellulosesz.core.service.DefaultServiceRegistry;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
final class CommandPipelineOutcomeTest {

    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "test",
            "home",
            "",
            CommandSourceKind.ANY
    );

    @Test
    void laterValidationStillRunsBeforeCostReservation() {
        var logger = new RecordingLogger();
        var costs = new FakeCostService();
        var pipeline = pipeline(logger);
        var terminalCalls = new AtomicInteger();

        pipeline.addMiddleware(new CommandCostMiddleware(costs), "cost");
        pipeline.addMiddleware(rejectingValidation(), "late-validation");

        var outcome = pipeline.execute(
                DESCRIPTOR,
                new FakeContext(),
                () -> {
                    terminalCalls.incrementAndGet();
                    return CompletableFuture.completedFuture(CommandOutcome.success());
                }
        ).toCompletableFuture().join();

        assertEquals(CommandOutcome.Status.REJECTED, outcome.status());
        assertEquals(0, costs.reserveCalls.get());
        assertEquals(0, terminalCalls.get());
        assertEquals(0, logger.errors.get());
    }

    private static DefaultCommandExecutionPipeline pipeline(RecordingLogger logger) {
        return new DefaultCommandExecutionPipeline(logger, new DefaultServiceRegistry());
    }

    private static CommandMiddleware rejectingValidation() {
        return (_, _, _) -> CompletableFuture.completedFuture(CommandOutcome.rejected());
    }

    @Test
    void asyncSuccessWaitsForTerminalAndCommitsOnce() {
        var costs = new FakeCostService();
        var reservation = costs.reservation;
        var pipeline = pipeline(new RecordingLogger());
        var terminal = new CompletableFuture<CommandOutcome>();
        pipeline.addMiddleware(new CommandCostMiddleware(costs), "cost");

        var result = pipeline.execute(
                DESCRIPTOR,
                new FakeContext(),
                () -> terminal
        ).toCompletableFuture();

        assertFalse(result.isDone());
        terminal.complete(CommandOutcome.success(7));

        assertEquals(CommandOutcome.success(7), result.join());
        assertEquals(1, reservation.commits.get());
        assertEquals(0, reservation.refunds.get());
    }

    @Test
    void everyNonSuccessOutcomeRefundsExactlyOnce() {
        for (var outcome : new CommandOutcome[]{
                CommandOutcome.rejected(),
                CommandOutcome.failed(),
                CommandOutcome.partial()
        }) {
            var costs = new FakeCostService();
            var pipeline = pipeline(new RecordingLogger());
            pipeline.addMiddleware(new CommandCostMiddleware(costs), "cost");

            var result = pipeline.execute(
                    DESCRIPTOR,
                    new FakeContext(),
                    () -> CompletableFuture.completedFuture(outcome)
            ).toCompletableFuture().join();

            assertSame(outcome, result);
            assertEquals(0, costs.reservation.commits.get());
            assertEquals(1, costs.reservation.refunds.get());
        }
    }

    @Test
    void exceptionalTerminalRefundsAndIsReportedAsFailed() {
        var logger = new RecordingLogger();
        var costs = new FakeCostService();
        var pipeline = pipeline(logger);
        pipeline.addMiddleware(new CommandCostMiddleware(costs), "cost");

        var outcome = pipeline.execute(
                DESCRIPTOR,
                new FakeContext(),
                () -> CompletableFuture.failedFuture(new IllegalStateException("terminal"))
        ).toCompletableFuture().join();

        assertEquals(CommandOutcome.Status.FAILED, outcome.status());
        assertEquals(0, costs.reservation.commits.get());
        assertEquals(1, costs.reservation.refunds.get());
        assertEquals(1, logger.errors.get());
    }

    @Test
    void refundFailureIsObservedAndChangesOutcomeToFailed() {
        var logger = new RecordingLogger();
        var costs = new FakeCostService();
        costs.reservation.refundFailure = new IllegalStateException("refund");
        var pipeline = pipeline(logger);
        pipeline.addMiddleware(new CommandCostMiddleware(costs), "cost");

        var outcome = pipeline.execute(
                DESCRIPTOR,
                new FakeContext(),
                () -> CompletableFuture.completedFuture(CommandOutcome.rejected())
        ).toCompletableFuture().join();

        assertEquals(CommandOutcome.Status.FAILED, outcome.status());
        assertEquals(1, costs.reservation.refunds.get());
        assertEquals(1, logger.errors.get());
    }

    @Test
    void observationMiddlewareDoesNotReplaceTerminalOutcome() {
        var observed = new AtomicInteger();
        var pipeline = pipeline(new RecordingLogger());
        pipeline.addMiddleware(
                new CommandMiddleware() {
                    @Override
                    public CommandMiddlewarePhase phase() {
                        return CommandMiddlewarePhase.OBSERVATION;
                    }

                    @Override
                    public CompletionStage<CommandOutcome> invoke(
                            CommandDescriptor descriptor,
                            CommandPolicyContext context,
                            top.likoslupus.cellulosesz.api.command.CommandContinuation continuation
                    ) {
                        observed.incrementAndGet();
                        return continuation.proceed();
                    }
                }, "audit"
        );

        var expected = CommandOutcome.partial(3);
        var actual = pipeline.execute(
                DESCRIPTOR,
                new FakeContext(),
                () -> CompletableFuture.completedFuture(expected)
        ).toCompletableFuture().join();

        assertSame(expected, actual);
        assertEquals(1, observed.get());
    }

    private static final class FakeCostService implements CommandCostService {

        private final AtomicInteger reserveCalls = new AtomicInteger();
        private final FakeReservation reservation = new FakeReservation();

        @Override
        public BigDecimal cost(String command) {
            return BigDecimal.ONE;
        }

        @Override
        public CompletionStage<CommandCostReserveResult> reserve(UUID uuid, String command) {
            reserveCalls.incrementAndGet();
            return CompletableFuture.completedFuture(
                    new CommandCostReserveResult.Reserved(reservation)
            );
        }

    }

    private static final class FakeReservation implements CommandCostReservation {

        private final AtomicInteger commits = new AtomicInteger();
        private final AtomicInteger refunds = new AtomicInteger();
        private @Nullable RuntimeException refundFailure;

        @Override
        public BigDecimal amount() {
            return BigDecimal.ONE;
        }

        @Override
        public CompletionStage<Void> commit() {
            commits.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> refund() {
            refunds.incrementAndGet();
            return refundFailure == null
                    ? CompletableFuture.completedFuture(null)
                    : CompletableFuture.failedFuture(refundFailure);
        }

    }

    private static final class FakeContext implements CommandPolicyContext {

        private static final UUID PLAYER = UUID.fromString(
                "00000000-0000-0000-0000-000000000001"
        );

        @Override
        public String invokedLabel() {
            return "home";
        }

        @Override
        public String canonicalRoot() {
            return "home";
        }

        @Override
        public boolean player() {
            return true;
        }

        @Override
        public Optional<UUID> playerUuid() {
            return Optional.of(PLAYER);
        }

        @Override
        public Optional<String> playerName() {
            return Optional.of("Player");
        }

        @Override
        public boolean hasPermission(String permission) {
            return false;
        }

        @Override
        public String auditSummary() {
            return "home";
        }

        @Override
        public void reply(LocalizedMessage message) {
        }

        @Override
        public void error(LocalizedMessage message) {
        }

    }

    private static final class RecordingLogger implements CellulosesZLogger {

        private final AtomicInteger errors = new AtomicInteger();

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
            errors.incrementAndGet();
        }

        @Override
        public void error(String message, Throwable throwable) {
            errors.incrementAndGet();
        }

        @Override
        public void info(String message) {
        }

    }

}
