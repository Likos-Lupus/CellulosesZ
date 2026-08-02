package top.likoslupus.cellulosesz.api.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;

import java.util.concurrent.CompletionStage;

public interface CommandExecutionPipeline {

    CompletionStage<CommandOutcome> execute(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal
    );

}
