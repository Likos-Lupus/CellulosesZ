package top.likoslupus.cellulosesz.api.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;

public interface CommandExecutionPipeline {

    int execute(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation terminal
    );

}
