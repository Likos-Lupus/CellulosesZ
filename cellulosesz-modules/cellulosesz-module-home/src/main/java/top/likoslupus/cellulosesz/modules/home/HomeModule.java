package top.likoslupus.cellulosesz.modules.home;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.home.application.DefaultHomeCommandService;
import top.likoslupus.cellulosesz.modules.home.application.HomeCommandService;
import top.likoslupus.cellulosesz.modules.home.command.HomeCommand;
import top.likoslupus.cellulosesz.modules.home.service.JsonHomeService;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "home",
        name = "Home",
        description = "Player home storage and teleport commands.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "teleport", "command"}
)
public final class HomeModule implements CellulosesZModule {

    private @Nullable HomeConfig config;
    private @Nullable HomeService homes;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.home",
                HomeConfig.class,
                "modules/home.yml",
                HomeConfig::new
        );
    }

    @Override
    @SuppressWarnings("resource")
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent();

        homes = new JsonHomeService(storage, root.resolve("homes"));
        context.services().register(HomeService.class, homes);
        context.services().register(JsonHomeService.class, (JsonHomeService) homes);
        context.services().register(HomeCommandService.class, new DefaultHomeCommandService(
                homes,
                context.services().require(TeleportService.class),
                context.services().require(CooldownService.class),
                context.services().require(PlayerResolver.class),
                context.services().require(PlayerLocationPlatformService.class),
                context.services().require(ServerThreadExecutor.class),
                requireNonNull(config, "HomeConfig has not been initialized")
        ));
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var service = context.services().require(HomeCommandService.class);
        context.track(registry.register("home-commands", new HomeCommand(service)));
    }

    @Override
    public void onReload(ModuleContext context) {
        config = context.configs().require("module.home", HomeConfig.class);
        context.services()
                .require(HomeCommandService.class)
                .configure(requireNonNull(config, "HomeConfig has not been initialized"));
    }

}
