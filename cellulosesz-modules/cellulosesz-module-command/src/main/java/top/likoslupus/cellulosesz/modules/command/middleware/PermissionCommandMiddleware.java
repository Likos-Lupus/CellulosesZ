package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.i18n.MessageService;

public final class PermissionCommandMiddleware implements CommandMiddleware {

    private final MessageService messages;

    public PermissionCommandMiddleware(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation continuation
    ) {
        if (!command.permission().isBlank() && !invocation.hasPermission(command.permission())) {
            invocation.error(messages.message("common.no-permission"));
            return 0;
        }
        return continuation.proceed();
    }

}
