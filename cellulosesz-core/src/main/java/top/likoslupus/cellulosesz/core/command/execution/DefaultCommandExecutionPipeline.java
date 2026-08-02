package top.likoslupus.cellulosesz.core.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.execution.*;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class DefaultCommandExecutionPipeline
        implements CommandExecutionPipeline, CommandMiddlewareRegistry {

    private static final Comparator<MiddlewareEntry> ORDER = Comparator
            .comparing((MiddlewareEntry entry) -> entry.middleware().phase())
            .thenComparingInt(entry -> entry.middleware().order())
            .thenComparingLong(MiddlewareEntry::sequence);

    private final CellulosesZLogger logger;
    private final ServiceRegistry services;
    private final List<MiddlewareEntry> middlewares = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    public DefaultCommandExecutionPipeline(
            CellulosesZLogger logger,
            ServiceRegistry services
    ) {
        this.logger = requireNonNull(logger, "logger");
        this.services = requireNonNull(services, "services");
    }

    private static Throwable unwrap(Throwable failure) {
        var current = failure;
        while ((
                current instanceof CompletionException
                        || current instanceof java.util.concurrent.ExecutionException
        )
                && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

    @Override
    public Registration addMiddleware(CommandMiddleware middleware, String owner) {
        var entry = new MiddlewareEntry(
                requireNonNull(middleware, "middleware"),
                requireNonBlank(owner, "owner"),
                sequence.getAndIncrement()
        );
        middlewares.add(entry);
        return new MiddlewareRegistration(entry);
    }

    @Override
    public List<CommandMiddleware> middlewares() {
        return middlewares.stream()
                .sorted(ORDER)
                .map(MiddlewareEntry::middleware)
                .toList();
    }

    @Override
    public CompletionStage<CommandOutcome> execute(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal
    ) {
        requireNonNull(descriptor, "descriptor");
        requireNonNull(context, "context");
        requireNonNull(terminal, "terminal");

        var stage = invoke(
                descriptor,
                context,
                terminal,
                middlewares(),
                0
        );
        return stage.handle((outcome, failure) -> {
            if (failure == null && outcome != null) {
                return outcome;
            }

            var cause = failure == null
                    ? new IllegalStateException("Command terminal completed without an outcome")
                    : unwrap(failure);
            reportInternalFailure(descriptor, context, cause);

            return CommandOutcome.failed();
        });
    }

    private CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal,
            List<CommandMiddleware> snapshot,
            int index
    ) {
        try {
            if (index >= snapshot.size()) {
                return requireNonNull(terminal.proceed(), "terminal stage");
            }

            var middleware = snapshot.get(index);
            return requireNonNull(
                    middleware.invoke(
                            descriptor,
                            context,
                            () -> invoke(descriptor, context, terminal, snapshot, index + 1)
                    ),
                    "middleware stage"
            );
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private void reportInternalFailure(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            Throwable failure
    ) {
        logger.error(
                "Command pipeline failed for /" + descriptor.canonicalName(),
                failure
        );
        var task = (Runnable) () -> context.error(LocalizedMessage.of(
                GeneratedMessageKeys.COMMANDS_COMMON_PLATFORM_INTERNAL_ERROR
        ));
        var serverThread = services
                .optional(ServerThreadExecutor.class)
                .orElse(null);

        if (serverThread == null || serverThread.isServerThread()) {
            runSafely(task, descriptor);
        } else {
            serverThread.execute(() -> runSafely(task, descriptor));
        }
    }

    private void runSafely(Runnable task, CommandDescriptor descriptor) {
        try {
            task.run();
        } catch (RuntimeException responseFailure) {
            logger.error(
                    "Failed to report command pipeline failure for /" + descriptor.canonicalName(),
                    responseFailure
            );
        }
    }

    private void remove(MiddlewareEntry entry) {
        middlewares.removeIf(candidate -> candidate == entry);
    }

    private record MiddlewareEntry(
            CommandMiddleware middleware,
            String owner,
            long sequence
    ) {

    }

    private final class MiddlewareRegistration implements Registration {

        private final MiddlewareEntry entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private MiddlewareRegistration(MiddlewareEntry entry) {
            this.entry = entry;
        }

        @Override
        public String owner() {
            return entry.owner();
        }

        @Override
        public boolean closed() {
            return closed.get();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                remove(entry);
            }
        }

    }

}
