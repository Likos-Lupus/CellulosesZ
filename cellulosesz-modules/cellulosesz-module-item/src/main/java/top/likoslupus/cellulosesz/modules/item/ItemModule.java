package top.likoslupus.cellulosesz.modules.item;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionRegistry;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.item.command.*;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemAutomationService;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemService;

import java.util.List;
import java.util.Map;

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
        requireNonNull(config, "ItemConfig has not been initialized").validate();
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        var loadedConfig = requireNonNull(config, "ItemConfig has not been initialized");
        var itemService = new DefaultItemService(platform, loadedConfig);
        var automationService = new DefaultItemAutomationService(
                platform,
                users,
                itemService,
                context.services().require(PlayerCommandDispatchService.class),
                loadedConfig
        );

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
        context.commands().register(new WorkstationCommand(platform, "disposal", List.of("trash"), "disposal"));
        context.commands().register(new WorkstationCommand(platform, "stonecutter", List.of(), "stonecutter"));

        var inventory = context.services().require(InventoryPlatformService.class);
        var recipes = context.services().require(RecipePlatformService.class);
        var users = context.services().require(UserService.class);
        context.commands().register(new MoreCommand(platform, inventory, loadedItems, loadedConfig));
        context.commands().register(new HatCommand(platform, inventory));
        context.commands().register(new PowerToolListCommand(platform, loadedAutomation));
        context.commands().register(new PowerToolToggleCommand(platform, loadedAutomation, users));
        context.commands().register(new ItemDbCommand(platform, inventory, loadedItems, loadedConfig));
        context.commands().register(new BookCommand(platform, inventory));
        context.commands().register(new SkullCommand(platform, inventory));
        context.commands().register(new ClearInventoryCommand(
                platform, inventory, loadedItems, users,
                context.services().require(ConfirmationService.class),
                context.services().require(PermissionService.class), loadedConfig
        ));
        context.commands().register(new ClearInventoryConfirmToggleCommand(
                platform, users, context.services().require(ConfirmationService.class)
        ));
        context.commands().register(new CondenseCommand(platform, loadedItems, recipes, loadedConfig));
        context.commands().register(new RecipeCommand(loadedItems, recipes, loadedConfig));
        registerCommandPermissions(context.services().require(PermissionCatalog.class));

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
        var candidate = context.configs().require("module.item", ItemConfig.class);
        candidate.validate();
        current.copyFrom(candidate);
        ((DefaultItemService) requireNonNull(items, "ItemService has not been initialized")).configure(current);
        ((DefaultItemAutomationService) requireNonNull(automation, "ItemAutomationService has not been initialized")).configure(current);
    }

    private static void registerCommandPermissions(PermissionCatalog catalog) {
        Map.ofEntries(
                Map.entry("cellulosesz.command.clearinventory.others", "Clear another player's inventory"),
                Map.entry("cellulosesz.command.clearinventory.all", "Clear all online player inventories"),
                Map.entry("cellulosesz.command.clearinventory.multiple", "Clear multiple player inventories"),
                Map.entry("cellulosesz.command.clearinventory.armor", "Include equipment in an inventory clear"),
                Map.entry("cellulosesz.command.clearinventory.bypass-confirm", "Bypass inventory clear confirmation"),
                Map.entry("cellulosesz.command.clearinventory.exempt", "Exempt a player from inventory clears"),
                Map.entry("cellulosesz.command.more.oversized", "Create permitted oversized held stacks"),
                Map.entry("cellulosesz.command.hat.ignore-binding", "Move binding-cursed helmets"),
                Map.entry("cellulosesz.command.book.title", "Change a held book title"),
                Map.entry("cellulosesz.command.book.author", "Change a held book author"),
                Map.entry("cellulosesz.command.book.others", "Modify a book signed by another author"),
                Map.entry("cellulosesz.command.skull.modify", "Change the profile of a held player head"),
                Map.entry("cellulosesz.command.skull.spawn", "Create a player head"),
                Map.entry("cellulosesz.command.skull.others", "Give or modify a head for another player"),
                Map.entry("cellulosesz.command.skull.spawn.others", "Create a player head for another player")
        ).forEach(catalog::register);
    }

}
