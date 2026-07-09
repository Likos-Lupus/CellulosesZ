package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.*;
import top.likoslupus.cellulosesz.api.i18n.MessageService;

public final class SourceKindCommandMiddleware implements CommandMiddleware {

    private final MessageService messages;

    public SourceKindCommandMiddleware(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation continuation
    ) {
        if (command.sourceKind() == CommandSourceKind.PLAYER_ONLY && !invocation.player()) {
            invocation.error(messages.message("common.player-only"));
            return 0;
        }
        if (command.sourceKind() == CommandSourceKind.CONSOLE_ONLY && invocation.player()) {
            invocation.error(messages.message("common.console-only"));
            return 0;
        }
        return continuation.proceed();
    }

}
