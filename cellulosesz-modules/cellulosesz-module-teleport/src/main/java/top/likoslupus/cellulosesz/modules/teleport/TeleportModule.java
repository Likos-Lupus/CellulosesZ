package top.likoslupus.cellulosesz.modules.teleport;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "teleport",
        name = "Teleport",
        description = "Teleport service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class TeleportModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.teleport",
                BasicModuleConfig.class,
                "modules/teleport.yml",
                BasicModuleConfig::new
        );
    }

}
