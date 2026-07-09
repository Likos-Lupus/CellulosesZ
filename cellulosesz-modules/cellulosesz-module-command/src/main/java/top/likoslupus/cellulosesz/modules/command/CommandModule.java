package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "command",
        name = "Command",
        description = "Registers the CellulosesZ root command and command infrastructure.",
        phase = ModulePhase.CORE,
        priority = 0
)
public final class CommandModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.command",
                BasicModuleConfig.class,
                "modules/command.yml",
                BasicModuleConfig::new
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        context.commands().register(new RootCellulosesZCommand(context));
        context.commands().register(new HelpCommand(context));
    }

}
