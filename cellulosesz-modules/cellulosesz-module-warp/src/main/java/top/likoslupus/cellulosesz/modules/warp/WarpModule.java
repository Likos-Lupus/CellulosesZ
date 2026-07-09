package top.likoslupus.cellulosesz.modules.warp;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "warp",
        name = "Warp",
        description = "Warp service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"teleport", "command"}
)
public final class WarpModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.warp",
                BasicModuleConfig.class,
                "modules/warp.yml",
                BasicModuleConfig::new
        );
    }

}
