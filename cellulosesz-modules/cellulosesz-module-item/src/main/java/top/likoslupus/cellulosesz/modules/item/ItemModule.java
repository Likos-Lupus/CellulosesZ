package top.likoslupus.cellulosesz.modules.item;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionRegistry;
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

import java.util.List;

import static java.util.Objects.requireNonNull;

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
        var loadedConfig = requireNonNull(config, "ItemConfig has not been initialized");
        var itemService = new DefaultItemService(platform, loadedConfig);
        var automationService = new DefaultItemAutomationService(platform, users, itemService, loadedConfig);

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
        var loadedItems = requireNonNull(items, "ItemService has not been initialized");
        var loadedAutomation = requireNonNull(automation, "ItemAutomationService has not been initialized");
        var loadedConfig = requireNonNull(config, "ItemConfig has not been initialized");

        context.commands().register(new ItemCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new GiveCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new EnchantCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new RepairCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new InvSeeCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new EnderChestCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new PowerToolCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new UnlimitedCommand(platform, loadedItems, loadedAutomation, loadedConfig));
        context.commands().register(new ItemNameCommand(platform));
        context.commands().register(new ItemLoreCommand(platform, loadedConfig));
        context.commands().register(new PotionCommand(platform));
        context.commands().register(new FireworkCommand(platform));
        context.commands().register(new WorkstationCommand(
                platform,
                "anvil",
                List.of(),
                "anvil"
        ));
        context.commands().register(new WorkstationCommand(
                platform,
                "cartographytable",
                List.of("cartography"),
                "cartography"
        ));
        context.commands().register(new WorkstationCommand(
                platform,
                "grindstone",
                List.of(),
                "grindstone"
        ));
        context.commands().register(new WorkstationCommand(
                platform,
                "loom",
                List.of(),
                "loom"
        ));
        context.commands().register(new WorkstationCommand(
                platform,
                "smithingtable",
                List.of("smithing"),
                "smithing"
        ));
        context.commands().register(new WorkstationCommand(
                platform,
                "workbench",
                List.of("craft", "wb"),
                "workbench"
        ));

        var suggestions = context.services().require(CommandSuggestionRegistry.class);
        List.of("item", "give", "worth", "sell", "setworth")
                .forEach(command ->
                        suggestions.register(
                                command,
                                "item",
                                ignored -> loadedItems.names()
                        )
                );
    }

    @Override
    public void onReload(ModuleContext context) {
        var current = requireNonNull(config, "ItemConfig has not been initialized");
        current.copyFrom(context.configs().require("module.item", ItemConfig.class));
        ((DefaultItemService) requireNonNull(items, "ItemService has not been initialized")).configure(current);
        ((DefaultItemAutomationService) requireNonNull(automation, "ItemAutomationService has not been initialized")).configure(current);
    }

}
