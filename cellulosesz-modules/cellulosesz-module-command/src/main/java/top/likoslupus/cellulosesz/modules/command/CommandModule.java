package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandAvailabilityService;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.runtime.RuntimeService;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.command.middleware.*;

import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "command",
        name = "Command",
        description = "Registers the CellulosesZ root command and command infrastructure.",
        phase = ModulePhase.CORE,
        priority = 0
)
public final class CommandModule implements CellulosesZModule {

    private @Nullable CommandConfig config;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = validate(context.configs().register(
                "module.command",
                CommandConfig.class,
                "modules/command.yml",
                CommandConfig::new
        ));
    }

    @Override
    public void registerServices(ModuleContext context) {
        var current = requireNonNull(config, "CommandConfig has not been initialized");
        context.services()
                .require(CommandAvailabilityService.class)
                .replaceDisabledCommands(current.disabledCommands);

        var middlewares = context.services().require(
                CommandMiddlewareRegistry.class
        );

        middlewares.addMiddleware(new SourceKindCommandMiddleware());
        middlewares.addMiddleware(new ModuleEnabledCommandMiddleware(context));
        middlewares.addMiddleware(new PermissionCommandMiddleware());
        middlewares.addMiddleware(new CommandCostMiddleware(
                context.services().require(CommandCostService.class),
                context.services().require(ServerThreadExecutor.class)
        ));

        if (current.auditCommands) {
            middlewares.addMiddleware(new AuditCommandMiddleware(context.logger()));
        }
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        context.track(registry.register(
                "cellulosesz-command",
                new CellulosesZCommand(
                        context.services().require(RuntimeService.class),
                        context.services().require(ServerThreadExecutor.class)
                )
        ));
        context.track(registry.register("help-command", new HelpCommand()));
    }

    @Override
    public void onReload(ModuleContext context) {
        config = validate(context.configs().require(
                "module.command",
                CommandConfig.class
        ));
        context.services()
                .require(CommandAvailabilityService.class)
                .replaceDisabledCommands(requireNonNull(config, "config").disabledCommands);
    }

    private static CommandConfig validate(CommandConfig config) {
        requirePositive(
                config.helpPageSize,
                "module.command.helpPageSize"
        );
        return config;
    }

}
