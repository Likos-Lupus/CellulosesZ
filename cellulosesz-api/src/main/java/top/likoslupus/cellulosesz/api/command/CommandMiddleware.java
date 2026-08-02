package top.likoslupus.cellulosesz.api.command;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;

import java.util.concurrent.CompletionStage;

public interface CommandMiddleware {

    default CommandMiddlewarePhase phase() {
        return CommandMiddlewarePhase.VALIDATION;
    }

    default int order() {
        return 0;
    }

    CompletionStage<CommandOutcome> invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    );

}
