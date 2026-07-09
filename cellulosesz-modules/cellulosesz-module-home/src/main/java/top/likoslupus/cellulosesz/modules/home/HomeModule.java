package top.likoslupus.cellulosesz.modules.home;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "home",
        name = "Home",
        description = "Home service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "teleport", "command"}
)
public final class HomeModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.home",
                BasicModuleConfig.class,
                "modules/home.yml",
                BasicModuleConfig::new
        );
    }

}
