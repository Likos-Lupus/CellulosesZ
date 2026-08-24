package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.command.CommandContinuation;
import top.likoslupus.cellulosesz.core.command.CommandMiddleware;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class SourceKindCommandMiddleware implements CommandMiddleware {

    @Override
    public CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    ) {
        if (descriptor.requiredSourceKind() == CommandSourceKind.PLAYER_ONLY
                && !context.player()
        ) {
            context.error(LocalizedMessage.of("common.player-only"));
            return CompletableFuture.completedFuture(CommandOutcome.rejected());
        }

        if (descriptor.requiredSourceKind() == CommandSourceKind.CONSOLE_ONLY
                && context.player()
        ) {
            context.error(LocalizedMessage.of("common.console-only"));
            return CompletableFuture.completedFuture(CommandOutcome.rejected());
        }

        return continuation.proceed();
    }

}
