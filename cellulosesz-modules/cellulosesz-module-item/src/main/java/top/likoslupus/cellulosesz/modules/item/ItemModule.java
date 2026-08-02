package top.likoslupus.cellulosesz.modules.item;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationService;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.command.service.PlayerChatDispatchService;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;
import top.likoslupus.cellulosesz.modules.item.application.ItemCommandService;
import top.likoslupus.cellulosesz.modules.item.application.WorkstationCommandService;
import top.likoslupus.cellulosesz.modules.item.command.*;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemAutomationService;
import top.likoslupus.cellulosesz.modules.item.service.DefaultItemService;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

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
    private @Nullable ItemRuntimeSettings runtimeSettings;
    private @Nullable ItemService items;
    private @Nullable ItemAutomationService automation;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.item",
                ItemConfig.class,
                "modules/item.yml",
                ItemConfig::new
        );
        var initial = context.configs().require("module.item", ItemConfig.class);
        initial.validate();
        config = copy(initial);
        runtimeSettings = new ItemRuntimeSettings(config);
    }

    @Override
    public void registerServices(ModuleContext context) {
        var itemPlatform = context.services().require(ItemPlatformService.class);
        var users = context.services().require(UserService.class);
        var loadedConfig = requireNonNull(config, "ItemConfig has not been initialized");
        var itemService = new DefaultItemService(itemPlatform, loadedConfig);
        var automationService = new DefaultItemAutomationService(
                itemPlatform,
                context.services().require(PlayerChatDispatchService.class),
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
        var registry = context.services().require(CommandRegistry.class);
        var itemPlatform = context.services().require(ItemPlatformService.class);
        var inventory = context.services().require(InventoryPlatformService.class);
        var workstations = context.services().require(WorkstationPlatformService.class);
        var players = context.services().require(PlayerDirectory.class);
        var recipes = context.services().require(RecipePlatformService.class);
        var users = context.services().require(UserService.class);
        var confirmations = context.services().require(ConfirmationService.class);
        var permissions = context.services().require(PermissionService.class);
        var loadedItems = requireNonNull(
                items,
                "ItemService has not been initialized"
        );
        var loadedAutomation = requireNonNull(
                automation,
                "ItemAutomationService has not been initialized"
        );
        var loadedConfig = requireNonNull(
                runtimeSettings,
                "ItemRuntimeSettings has not been initialized"
        );

        var itemCommands = new ItemCommandService(loadedItems, itemPlatform);
        var inventoryCommands = new InventoryCommandService(inventory);
        var workstationCommands = new WorkstationCommandService(workstations);

        track(
                context, registry,
                "item-command",
                new ItemCommand(itemCommands, loadedItems)
        );
        track(
                context, registry,
                "give-command",
                new GiveCommand(itemCommands, loadedItems)
        );
        track(
                context, registry,
                "enchant-command",
                new EnchantCommand(itemCommands)
        );
        track(
                context, registry,
                "repair-command",
                new RepairCommand(itemCommands)
        );
        track(
                context, registry,
                "invsee-command",
                new InvSeeCommand(inventoryCommands)
        );
        track(
                context, registry,
                "enderchest-command",
                new EnderChestCommand(inventoryCommands)
        );
        track(
                context, registry,
                "powertool-command",
                new PowerToolCommand(loadedAutomation, loadedItems)
        );
        track(
                context, registry,
                "unlimited-command",
                new UnlimitedCommand(loadedAutomation, loadedItems)
        );
        track(
                context, registry,
                "itemname-command",
                new ItemNameCommand(itemCommands)
        );
        track(
                context, registry,
                "itemlore-command",
                new ItemLoreCommand(itemCommands, loadedConfig)
        );
        track(
                context, registry,
                "potion-command",
                new PotionCommand(itemCommands)
        );
        track(
                context, registry,
                "firework-command",
                new FireworkCommand(itemCommands)
        );
        trackWorkstations(
                context,
                registry,
                workstationCommands
        );
        track(
                context, registry,
                "more-command",
                new MoreCommand(inventoryCommands, inventory, loadedConfig)
        );
        track(
                context, registry,
                "hat-command",
                new HatCommand(inventoryCommands)
        );
        track(
                context, registry,
                "powertoollist-command",
                new PowerToolListCommand(loadedAutomation)
        );
        track(
                context, registry,
                "powertooltoggle-command",
                new PowerToolToggleCommand(loadedAutomation)
        )
        ;
        track(
                context, registry,
                "itemdb-command",
                new ItemDbCommand(inventory, loadedItems)
        );
        track(
                context, registry,
                "book-command",
                new BookCommand(inventoryCommands, inventory)
        );
        track(
                context, registry,
                "skull-command",
                new SkullCommand(inventoryCommands, inventory)
        );
        track(
                context, registry,
                "clearinventory-command",
                new ClearInventoryCommand(
                        inventory,
                        loadedItems,
                        users,
                        confirmations,
                        permissions,
                        players,
                        loadedConfig,
                        Clock.systemUTC()
                )
        );
        track(
                context, registry,
                "clearinventoryconfirmtoggle-command",
                new ClearInventoryConfirmToggleCommand(users, confirmations)
        );
        track(
                context, registry,
                "condense-command",
                new CondenseCommand(loadedItems, inventory, recipes, loadedConfig)
        );
        track(
                context, registry,
                "recipe-command",
                new RecipeCommand(loadedItems, recipes, loadedConfig)
        );

        registerCommandPermissions(context.services().require(PermissionCatalog.class));
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var previous = requireNonNull(config, "ItemConfig has not been initialized");
        var loaded = reload.configs().require("module.item", ItemConfig.class);
        loaded.validate();

        var candidate = copy(loaded);
        var itemService = (DefaultItemService) requireNonNull(
                items,
                "ItemService has not been initialized"
        );
        var automationService = (DefaultItemAutomationService) requireNonNull(
                automation,
                "ItemAutomationService has not been initialized"
        );
        var preparedItems = itemService.prepareConfiguration(candidate);

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    itemService.configure(preparedItems);
                    try {
                        automationService.configure(candidate);
                    } catch (RuntimeException failure) {
                        itemService.configure(previous);
                        throw failure;
                    }
                    requireNonNull(
                            runtimeSettings,
                            "ItemRuntimeSettings has not been initialized"
                    ).configure(candidate);
                    config = candidate;
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    itemService.configure(previous);
                    automationService.configure(previous);
                    requireNonNull(
                            runtimeSettings,
                            "ItemRuntimeSettings has not been initialized"
                    ).configure(previous);
                    config = previous;
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

    private static void track(
            ModuleContext context,
            CommandRegistry registry,
            String id,
            CommandContributor contributor
    ) {
        context.scope().own(registry.register(id, contributor));
    }

    private static void trackWorkstations(
            ModuleContext context,
            CommandRegistry registry,
            WorkstationCommandService service
    ) {
        track(
                context, registry,
                "anvil-command",
                workstation(
                        service,
                        "anvil",
                        List.of(),
                        WorkstationKind.ANVIL
                )
        );
        track(
                context, registry,
                "cartographytable-command",
                workstation(
                        service,
                        "cartographytable",
                        List.of("cartography"),
                        WorkstationKind.CARTOGRAPHY
                )
        );
        track(
                context, registry,
                "disposal-command",
                workstation(
                        service,
                        "disposal",
                        List.of("trash"),
                        WorkstationKind.DISPOSAL
                )
        );
        track(
                context, registry,
                "grindstone-command",
                workstation(
                        service,
                        "grindstone",
                        List.of(),
                        WorkstationKind.GRINDSTONE
                )
        );
        track(
                context, registry,
                "loom-command",
                workstation(
                        service,
                        "loom",
                        List.of(),
                        WorkstationKind.LOOM
                )
        );
        track(
                context, registry,
                "smithingtable-command",
                workstation(
                        service,
                        "smithingtable",
                        List.of("smithing"),
                        WorkstationKind.SMITHING
                )
        );
        track(
                context, registry,
                "stonecutter-command",
                workstation(
                        service,
                        "stonecutter",
                        List.of(),
                        WorkstationKind.STONECUTTER
                )
        );
        track(
                context, registry,
                "workbench-command",
                workstation(
                        service,
                        "workbench",
                        List.of("craft", "wb"),
                        WorkstationKind.WORKBENCH
                )
        );
    }

    private static void registerCommandPermissions(PermissionCatalog catalog) {
        Map.ofEntries(
                Map.entry(
                        "cellulosesz.command.clearinventory.others",
                        "Clear another player's inventory"
                ),
                Map.entry(
                        "cellulosesz.command.clearinventory.all",
                        "Clear all online player inventories"
                ),
                Map.entry(
                        "cellulosesz.command.clearinventory.multiple",
                        "Clear multiple player inventories"
                ),
                Map.entry(
                        "cellulosesz.command.clearinventory.armor",
                        "Include equipment in an inventory clear"
                ),
                Map.entry(
                        "cellulosesz.command.clearinventory.bypass-confirm",
                        "Bypass inventory clear confirmation"
                ),
                Map.entry(
                        "cellulosesz.command.clearinventory.exempt",
                        "Exempt a player from inventory clears"
                ),
                Map.entry(
                        "cellulosesz.command.more.oversized",
                        "Create permitted oversized held stacks"
                ),
                Map.entry(
                        "cellulosesz.command.hat.ignore-binding",
                        "Move binding-cursed helmets"
                ),
                Map.entry(
                        "cellulosesz.command.book.title",
                        "Change a held book title"
                ),
                Map.entry(
                        "cellulosesz.command.book.author",
                        "Change a held book author"
                ),
                Map.entry(
                        "cellulosesz.command.book.others",
                        "Modify a book signed by another author"
                ),
                Map.entry(
                        "cellulosesz.command.skull.modify",
                        "Change the profile of a held player head"
                ),
                Map.entry(
                        "cellulosesz.command.skull.spawn",
                        "Create a player head"
                ),
                Map.entry(
                        "cellulosesz.command.skull.others",
                        "Give or modify a head for another player"
                ),
                Map.entry(
                        "cellulosesz.command.skull.spawn.others",
                        "Create a player head for another player"
                )
        ).forEach(catalog::register);
    }

    private static WorkstationCommand workstation(
            WorkstationCommandService service,
            String root,
            List<String> aliases,
            WorkstationKind kind
    ) {
        return new WorkstationCommand(service, root, aliases, kind);
    }

    private static ItemConfig copy(ItemConfig source) {
        var copy = new ItemConfig();
        copy.copyFrom(source);
        copy.validate();
        return copy;
    }

}
