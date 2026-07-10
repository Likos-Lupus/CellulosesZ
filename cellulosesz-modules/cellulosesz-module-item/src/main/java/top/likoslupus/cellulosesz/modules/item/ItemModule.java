package top.likoslupus.cellulosesz.modules.item;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemService;

@CellulosesModule(
        id = "item",
        name = "Item",
        description = "Basic item descriptor parsing and item delivery service.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class ItemModule implements CellulosesZModule {

    private @Nullable ItemConfig config;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.item",
                ItemConfig.class,
                "modules/item.yml",
                ItemConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var items = new DefaultItemService(platform);
        context.services().register(ItemService.class, items);
        context.services().register(DefaultItemService.class, items);
    }

}
