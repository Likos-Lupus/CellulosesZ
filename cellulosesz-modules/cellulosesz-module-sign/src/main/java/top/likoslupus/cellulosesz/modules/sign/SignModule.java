package top.likoslupus.cellulosesz.modules.sign;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "sign",
        name = "Sign",
        description = "Sign handler placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"command", "economy", "item"},
        enabledByDefault = false
)
public final class SignModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.sign",
                BasicModuleConfig.class,
                "modules/sign.yml",
                BasicModuleConfig::new
        );
    }

}
