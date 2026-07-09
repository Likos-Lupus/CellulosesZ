package top.likoslupus.cellulosesz.modules.item;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.core.config.BasicModuleConfig;

@CellulosesModule(
        id = "item",
        name = "Item",
        description = "Item service placeholder.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class ItemModule implements CellulosesZModule {

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.item",
                BasicModuleConfig.class,
                "modules/item.yml",
                BasicModuleConfig::new
        );
    }

}
