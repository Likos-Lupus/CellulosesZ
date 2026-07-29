package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.Map;

public final class ModuleEnabledCommandMiddleware implements CommandMiddleware {

    private final ModuleContext context;

    public ModuleEnabledCommandMiddleware(ModuleContext context) {
        this.context = context;
    }

    @Override
    public int invoke(
            CommandDescriptor descriptor,
            CommandPolicyContext policy,
            CommandContinuation continuation
    ) {
        var moduleId = descriptor.moduleId();
        if (!"unknown".equals(moduleId) && !context.moduleEnabled(moduleId)) {
            policy.error(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMON_MODULE_DISABLED,
                    Map.of("module", moduleId)
            ));
            return 0;
        }
        return continuation.proceed();
    }

}
