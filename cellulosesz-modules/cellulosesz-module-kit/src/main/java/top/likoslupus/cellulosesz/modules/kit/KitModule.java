package top.likoslupus.cellulosesz.modules.kit;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionContext;
import top.likoslupus.cellulosesz.api.command.service.CommandSuggestionRegistry;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.kit.command.*;
import top.likoslupus.cellulosesz.modules.kit.service.DefaultKitService;

import java.util.Collection;
import java.util.function.Function;

@CellulosesModule(
        id = "kit",
        name = "Kit",
        description = "Kit storage, preview, claim, cooldown, and cost services.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command", "item"},
        optional = {"economy"}
)
public final class KitModule implements CellulosesZModule {

    private @Nullable KitConfig config;
    private @Nullable KitService kits;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.kit",
                KitConfig.class,
                "modules/kit.yml",
                KitConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var users = context.services().require(UserService.class);
        var items = context.services().require(ItemService.class);
        var economy = context.services().optional(EconomyService.class);
        var root = context.dataDirectory().getParent().resolve("kits");

        kits = new DefaultKitService(storage, users, items, economy, config, root);
        context.services().register(KitService.class, kits);
        context.services().register(DefaultKitService.class, (DefaultKitService) kits);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var users = context.services().require(UserService.class);
        var items = context.services().require(ItemService.class);

        context.commands().register(new KitCommand(platform, kits));
        context.commands().register(new ShowKitCommand(platform, kits));
        context.commands().register(new CreateKitCommand(platform, kits, items));
        context.commands().register(new DelKitCommand(platform, kits));
        context.commands().register(new KitResetCommand(platform, kits, users));

        var suggestions = context.services().require(CommandSuggestionRegistry.class);
        var kitNames = (Function<CommandSuggestionContext, Collection<String>>) _ ->
                kits.kits().stream()
                        .map(kit -> kit.id)
                        .toList();
        suggestions.register("kit", "name", kitNames);
        suggestions.register("showkit", "name", kitNames);
        suggestions.register("delkit", "name", kitNames);
        suggestions.register("kitreset", "kit", kitNames);
    }

    @Override
    public void onReload(ModuleContext context) {
        if (kits != null) {
            kits.reload();
        }
    }

}
