package top.likoslupus.cellulosesz.modules.kit;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "kit",
        name = "Kit",
        description = "Kit service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command", "item"}
)
public final class KitModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.kit",
                BasicModuleConfig.class,
                "modules/kit.yml",
                BasicModuleConfig::new
        );
    }

}
