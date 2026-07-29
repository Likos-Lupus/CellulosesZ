package top.likoslupus.cellulosesz.modules.command;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.command.DefaultCommandRegistry;
import top.likoslupus.cellulosesz.modules.command.middleware.*;

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
        config = context.configs().register(
                "module.command",
                CommandConfig.class,
                "modules/command.yml",
                CommandConfig::new
        );
        requireNonNull(config, "CommandConfig has not been initialized");
    }

    @Override
    public void registerServices(ModuleContext context) {
        var registry = context.services().require(DefaultCommandRegistry.class);
        registry.disabledCommands(config.disabledCommands);

        var middlewares = context.services().require(CommandMiddlewareRegistry.class);
        middlewares.addMiddleware(new SourceKindCommandMiddleware());
        middlewares.addMiddleware(new ModuleEnabledCommandMiddleware(context));
        middlewares.addMiddleware(new PermissionCommandMiddleware());
        middlewares.addMiddleware(new CommandCostMiddleware(
                context.services().require(CommandCostService.class),
                context.services().require(ServerThreadExecutor.class)
        ));

        if (config.auditCommands) {
            middlewares.addMiddleware(new AuditCommandMiddleware(context.logger()));
        }
    }

    @Override
    public void registerCommands(ModuleContext context) {
        context.commands().register(new RootCellulosesZCommand(context));
        context.commands().register(new HelpCommand(context, config));
    }

    @Override
    public void onReload(ModuleContext context) {
        config = context.configs().require("module.command", CommandConfig.class);
        context.services().require(DefaultCommandRegistry.class).disabledCommands(config.disabledCommands);
    }

}
