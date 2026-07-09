package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.CommandMiddlewareRegistry;
import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.command.DefaultCommandRegistry;
import top.likoslupus.cellulosesz.modules.command.middleware.AuditCommandMiddleware;
import top.likoslupus.cellulosesz.modules.command.middleware.ModuleEnabledCommandMiddleware;
import top.likoslupus.cellulosesz.modules.command.middleware.PermissionCommandMiddleware;
import top.likoslupus.cellulosesz.modules.command.middleware.SourceKindCommandMiddleware;

@CellulosesModule(
        id = "command",
        name = "Command",
        description = "Registers the CellulosesZ root command and command infrastructure.",
        phase = ModulePhase.CORE,
        priority = 0
)
public final class CommandModule implements CellulosesZModule {

    private CommandConfig config;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.command",
                CommandConfig.class,
                "modules/command.yml",
                CommandConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var registry = context.services().require(DefaultCommandRegistry.class);
        registry.disabledCommands(config.disabledCommands);

        var middlewares = context.services().require(CommandMiddlewareRegistry.class);
        var messages = context.services().require(MessageService.class);

        middlewares.addMiddleware(new SourceKindCommandMiddleware(messages));
        middlewares.addMiddleware(new ModuleEnabledCommandMiddleware(context));
        middlewares.addMiddleware(new PermissionCommandMiddleware(messages));

        if (config.auditCommands) {
            middlewares.addMiddleware(new AuditCommandMiddleware(context.logger()));
        }
    }

    @Override
    public void registerCommands(ModuleContext context) {
        context.commands().register(new RootCellulosesZCommand(context));
        context.commands().register(new HelpCommand(context));
    }

}
