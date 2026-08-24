package top.likoslupus.cellulosesz.core.command.execution;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.core.command.CommandContinuation;

import java.util.concurrent.CompletionStage;

public interface CommandExecutionPipeline {

    CompletionStage<CommandOutcome> execute(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal
    );

}
