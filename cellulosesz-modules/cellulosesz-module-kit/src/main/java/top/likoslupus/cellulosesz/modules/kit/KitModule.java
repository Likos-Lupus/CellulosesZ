package top.likoslupus.cellulosesz.modules.kit;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.kit.application.DefaultKitCommandService;
import top.likoslupus.cellulosesz.modules.kit.application.KitCommandService;
import top.likoslupus.cellulosesz.modules.kit.command.KitCommand;
import top.likoslupus.cellulosesz.modules.kit.service.DefaultKitService;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "kit",
        name = "Kit",
        description = "Kit storage, preview, claim, cooldown, and cost services.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"},
        optional = {"economy"}
)
@SuppressWarnings("resource")
public final class KitModule implements CellulosesZModule {

    private @Nullable KitConfig config;
    private @Nullable KitService kits;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.kit",
                KitConfig.class,
                "modules/kit.yml",
                KitConfig::new
        );
        config = context.configs().require("module.kit", KitConfig.class);
    }

    @Override
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var users = context.services().require(UserService.class);
        var inventory = context.services().require(InventoryPlatformService.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);
        var economy = context.services().optional(EconomyService.class);
        var root = context.dataDirectory().getParent().resolve("kits");

        requireNonNull(config, "KitConfig has not been initialized");

        kits = new DefaultKitService(
                storage,
                users,
                inventory,
                serverThread,
                economy,
                config,
                root
        );
        context.services().register(KitService.class, kits);
        context.services().register(
                DefaultKitService.class,
                (DefaultKitService) kits
        );
        context.services().register(
                KitCommandService.class,
                new DefaultKitCommandService(
                        kits,
                        context.services().require(InventoryPlatformService.class),
                        context.services().require(PlayerResolver.class),
                        context.services().require(ServerThreadExecutor.class)
                )
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var service = context.services().require(KitCommandService.class);
        context.scope().own(registry.register("kit-commands", new KitCommand(service)));
    }

    @Override
    public void onReload(ModuleContext context) {
        if (kits != null) {
            kits.reload();
        }
    }

}
