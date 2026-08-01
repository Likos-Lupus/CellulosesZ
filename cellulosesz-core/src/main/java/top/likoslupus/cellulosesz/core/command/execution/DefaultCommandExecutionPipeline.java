package top.likoslupus.cellulosesz.core.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandExecutionPipeline;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class DefaultCommandExecutionPipeline implements
        CommandExecutionPipeline,
        CommandMiddlewareRegistry {

    private final CellulosesZLogger logger;
    private final List<MiddlewareEntry> middlewares = new CopyOnWriteArrayList<>();

    public DefaultCommandExecutionPipeline(CellulosesZLogger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public Registration addMiddleware(CommandMiddleware middleware, String owner) {
        var entry = new MiddlewareEntry(
                requireNonNull(middleware, "middleware"),
                requireNonBlank(owner, "owner")
        );
        middlewares.add(entry);
        return new MiddlewareRegistration(entry);
    }

    @Override
    public List<CommandMiddleware> middlewares() {
        return middlewares.stream().map(MiddlewareEntry::middleware).toList();
    }

    @Override
    public int execute(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal
    ) {
        requireNonNull(descriptor, "descriptor");
        requireNonNull(context, "context");
        requireNonNull(terminal, "terminal");
        return invoke(descriptor, context, terminal, middlewares(), 0);
    }

    private int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal,
            List<CommandMiddleware> snapshot,
            int index
    ) {
        try {
            if (index >= snapshot.size()) {
                return terminal.proceed();
            }

            var middleware = snapshot.get(index);
            return middleware.invoke(
                    descriptor,
                    context,
                    () -> invoke(descriptor, context, terminal, snapshot, index + 1)
            );
        } catch (RuntimeException failure) {
            logger.error(
                    "Command pipeline failed for /" + descriptor.canonicalName(),
                    failure
            );
            context.error(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_COMMON_PLATFORM_INTERNAL_ERROR
            ));
            return 0;
        }
    }

    private void remove(MiddlewareEntry entry) {
        middlewares.removeIf(candidate -> candidate == entry);
    }

    private record MiddlewareEntry(
            CommandMiddleware middleware,
            String owner
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
