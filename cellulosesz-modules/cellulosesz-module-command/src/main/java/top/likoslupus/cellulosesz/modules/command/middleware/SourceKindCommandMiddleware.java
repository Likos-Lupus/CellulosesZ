package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

public final class SourceKindCommandMiddleware implements CommandMiddleware {

    @Override
    public int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext context,
            CommandContinuation continuation
    ) {
        if (descriptor.requiredSourceKind() == CommandSourceKind.PLAYER_ONLY && !context.player()) {
            context.error(LocalizedMessage.of(GeneratedMessageKeys.COMMON_PLAYER_ONLY));
            return 0;
        }
        if (descriptor.requiredSourceKind() == CommandSourceKind.CONSOLE_ONLY && context.player()) {
            context.error(LocalizedMessage.of(GeneratedMessageKeys.COMMON_CONSOLE_ONLY));
            return 0;
        }
        return continuation.proceed();
    }

}
