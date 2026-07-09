package top.likoslupus.cellulosesz.modules.admin;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "admin",
        name = "Admin",
        description = "Administration service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class AdminModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.admin",
                BasicModuleConfig.class,
                "modules/admin.yml",
                BasicModuleConfig::new
        );
    }

}
