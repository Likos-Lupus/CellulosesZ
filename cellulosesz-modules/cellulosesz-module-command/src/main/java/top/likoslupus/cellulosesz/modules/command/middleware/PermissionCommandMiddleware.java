package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.command.CommandContinuation;
import top.likoslupus.cellulosesz.core.command.CommandMiddleware;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PermissionCommandMiddleware implements CommandMiddleware {

    @Override
    public CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    ) {
        if (!descriptor.permission().isBlank()
                && !context.hasPermission(descriptor.permission())
        ) {
            context.error(LocalizedMessage.of("common.no-permission"));
            return CompletableFuture.completedFuture(CommandOutcome.rejected());
        }

        return continuation.proceed();
    }

}
