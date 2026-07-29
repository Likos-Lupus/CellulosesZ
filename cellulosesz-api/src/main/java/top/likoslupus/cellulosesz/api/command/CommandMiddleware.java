package top.likoslupus.cellulosesz.api.command;

import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;

public interface CommandMiddleware {

    int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    );

}
