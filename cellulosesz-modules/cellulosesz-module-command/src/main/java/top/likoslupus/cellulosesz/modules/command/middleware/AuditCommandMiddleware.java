package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.util.Arrays;

public final class AuditCommandMiddleware implements CommandMiddleware {

    private final CellulosesZLogger logger;

    public AuditCommandMiddleware(CellulosesZLogger logger) {
        this.logger = logger;
    }

    @Override
    public int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation continuation
    ) {
        logger.debug("Command /%s %s".formatted(invocation.label(), String.join(" ", Arrays.asList(invocation.args()))));
        return continuation.proceed();
    }

}
