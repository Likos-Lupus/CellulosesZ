package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.core.command.CommandContinuation;
import top.likoslupus.cellulosesz.core.command.CommandMiddleware;
import top.likoslupus.cellulosesz.core.module.ModuleContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ModuleEnabledCommandMiddleware implements CommandMiddleware {

    private final ModuleContext context;

    public ModuleEnabledCommandMiddleware(ModuleContext context) {
        this.context = context;
    }

    @Override
    public CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext policy,
            CommandContinuation continuation
    ) {
        var moduleId = descriptor.moduleId();
        if (!"unknown".equals(moduleId) && !context.moduleEnabled(moduleId)) {
            policy.error(LocalizedMessage.of(
                    "common.module-disabled",
                    MessageArguments.empty()
            ));

            return CompletableFuture.completedFuture(CommandOutcome.rejected());
        }

        return continuation.proceed();
    }

}
