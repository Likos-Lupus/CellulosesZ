package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

public final class PermissionCommandMiddleware implements CommandMiddleware {

    @Override
    public int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    ) {
        if (!descriptor.permission().isBlank() && !context.hasPermission(descriptor.permission())) {
            context.error(LocalizedMessage.of(GeneratedMessageKeys.COMMON_NO_PERMISSION));
            return 0;
        }
        return continuation.proceed();
    }

}
