package top.likoslupus.cellulosesz.modules.home;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.modules.home.command.DelHomeCommand;
import top.likoslupus.cellulosesz.modules.home.command.HomeCommand;
import top.likoslupus.cellulosesz.modules.home.command.RenameHomeCommand;
import top.likoslupus.cellulosesz.modules.home.command.SetHomeCommand;
import top.likoslupus.cellulosesz.modules.home.service.JsonHomeService;

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
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent();

        homes = new JsonHomeService(storage, root.resolve("homes"));
        context.services().register(HomeService.class, homes);
        context.services().register(JsonHomeService.class, (JsonHomeService) homes);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var teleports = context.services().require(TeleportService.class);

        context.commands().register(new HomeCommand(platform, homes, teleports, config));
        context.commands().register(new SetHomeCommand(platform, homes, teleports, config));
        context.commands().register(new DelHomeCommand(platform, homes, teleports, config));
        context.commands().register(new RenameHomeCommand(platform, homes, teleports, config));
    }

}
