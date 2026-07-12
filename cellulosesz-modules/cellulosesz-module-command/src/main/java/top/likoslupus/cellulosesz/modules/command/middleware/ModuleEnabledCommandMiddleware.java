package top.likoslupus.cellulosesz.modules.command.middleware;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandContinuation;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.module.ModuleContext;

import java.util.Map;

public final class ModuleEnabledCommandMiddleware implements CommandMiddleware {

    private final ModuleContext context;

    public ModuleEnabledCommandMiddleware(ModuleContext context) {
        this.context = context;
    }

    @Override
    public int invoke(
            CellCommand command,
            CommandInvocation invocation,
            CommandContinuation continuation
    ) {
        var moduleId = context.commands().moduleId(command).orElse("unknown");
        if (!"unknown".equals(moduleId) && !context.moduleEnabled(moduleId)) {
            invocation.errorKey(
                    "common.module-disabled",
                    Map.of("module", moduleId)
            );
            return 0;
        }
        return continuation.proceed();
    }

}
