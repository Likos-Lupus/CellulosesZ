package top.likoslupus.cellulosesz.modules.user;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "user",
        name = "User",
        description = "User cache and profile foundation.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class UserModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.user",
                BasicModuleConfig.class,
                "modules/user.yml",
                BasicModuleConfig::new
        );
    }

}
