package top.likoslupus.cellulosesz.modules.item;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.item.command.*;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemAutomationService;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemService;

@CellulosesModule(
        id = "item",
        name = "Item",
        description = "Complex item descriptors, inventory utilities, power tools, and unlimited items.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class ItemModule implements CellulosesZModule {

    private @Nullable ItemConfig config;
    private @Nullable ItemService items;
    private @Nullable ItemAutomationService automation;

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
        var users = context.services().require(UserService.class);
        var itemService = new DefaultItemService(platform, config);
        var automationService = new DefaultItemAutomationService(platform, users, itemService, config);
        items = itemService;
        automation = automationService;
        context.services().register(ItemService.class, itemService);
        context.services().register(DefaultItemService.class, itemService);
        context.services().register(ItemAutomationService.class, automationService);
        context.services().register(DefaultItemAutomationService.class, automationService);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        context.commands().register(new ItemCommand(platform, items, automation, config));
        context.commands().register(new GiveCommand(platform, items, automation, config));
        context.commands().register(new EnchantCommand(platform, items, automation, config));
        context.commands().register(new RepairCommand(platform, items, automation, config));
        context.commands().register(new InvSeeCommand(platform, items, automation, config));
        context.commands().register(new EnderChestCommand(platform, items, automation, config));
        context.commands().register(new PowerToolCommand(platform, items, automation, config));
        context.commands().register(new UnlimitedCommand(platform, items, automation, config));
    }

}
