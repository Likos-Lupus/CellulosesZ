package top.likoslupus.cellulosesz.modules.text;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "text",
        name = "Text",
        description = "Text command placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class TextModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.text",
                BasicModuleConfig.class,
                "modules/text.yml",
                BasicModuleConfig::new
        );
    }

}
