package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.core.command.CommandContinuation;
import top.likoslupus.cellulosesz.core.command.CommandMiddleware;
import top.likoslupus.cellulosesz.core.command.CommandMiddlewarePhase;
import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger;

import java.util.concurrent.CompletionStage;

public final class AuditCommandMiddleware implements CommandMiddleware {

    private final CellulosesZLogger logger;

    public AuditCommandMiddleware(CellulosesZLogger logger) {
        this.logger = logger;
    }

    @Override
    public CommandMiddlewarePhase phase() {
        return CommandMiddlewarePhase.OBSERVATION;
    }

    @Override
    public CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    ) {
        logger.debug("Command /%s canonical=%s %s".formatted(
                context.invokedLabel(),
                descriptor.canonicalName(),
                context.auditSummary()
        ));
        return continuation.proceed();
    }

}
