package top.likoslupus.cellulosesz.modules.economy;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "economy",
        name = "Economy",
        description = "Economy service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class EconomyModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.economy",
                BasicModuleConfig.class,
                "modules/economy.yml",
                BasicModuleConfig::new
        );
    }

}
