package top.likoslupus.cellulosesz.modules.world;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "world",
        name = "World",
        description = "World command placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class WorldModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.world",
                BasicModuleConfig.class,
                "modules/world.yml",
                BasicModuleConfig::new
        );
    }

}
