package top.likoslupus.cellulosesz.core.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandExecutionPipeline;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

public final class DefaultCommandExecutionPipeline implements
        CommandExecutionPipeline,
        CommandMiddlewareRegistry {

    private final CellulosesZLogger logger;
    private final List<CommandMiddleware> middlewares = new CopyOnWriteArrayList<>();

    public DefaultCommandExecutionPipeline(CellulosesZLogger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public void addMiddleware(CommandMiddleware middleware) {
        middlewares.add(requireNonNull(middleware, "middleware"));
    }

    @Override
    public List<CommandMiddleware> middlewares() {
        return List.copyOf(middlewares);
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
        return invoke(descriptor, context, terminal, 0);
    }

    private int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal,
            int index
    ) {
        try {
            if (index >= middlewares.size()) return terminal.proceed();
            var middleware = middlewares.get(index);
            return middleware.invoke(
                    descriptor,
                    context,
                    () -> invoke(descriptor, context, terminal, index + 1)
            );
        } catch (RuntimeException failure) {
            logger.error("Command pipeline failed for /" + descriptor.canonicalName(), failure);
            context.error(LocalizedMessage.of(GeneratedMessageKeys.COMMANDS_COMMON_PLATFORM_INTERNAL_ERROR));
            return 0;
        }
    }

}
