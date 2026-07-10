package top.likoslupus.cellulosesz.modules.warp;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.command.DelWarpCommand;
import top.likoslupus.cellulosesz.modules.warp.command.SetWarpCommand;
import top.likoslupus.cellulosesz.modules.warp.command.WarpCommand;
import top.likoslupus.cellulosesz.modules.warp.command.WarpInfoCommand;
import top.likoslupus.cellulosesz.modules.warp.service.JsonWarpService;

@CellulosesModule(
        id = "warp",
        name = "Warp",
        description = "Named shared teleport locations.",
        phase = ModulePhase.FEATURE,
        requires = {"teleport", "command"}
)
public final class WarpModule implements CellulosesZModule {

    private @Nullable WarpConfig config;
    private @Nullable WarpService warps;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.warp",
                WarpConfig.class,
                "modules/warp.yml",
                WarpConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent();
        warps = new JsonWarpService(storage, root.resolve("warps"));
        warps.reload().join();
        context.services().register(WarpService.class, warps);
        context.services().register(JsonWarpService.class, (JsonWarpService) warps);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var teleports = context.services().require(TeleportService.class);

        context.commands().register(new WarpCommand(platform, warps, teleports, config));
        context.commands().register(new SetWarpCommand(platform, warps, teleports, config));
        context.commands().register(new DelWarpCommand(platform, warps, teleports, config));
        context.commands().register(new WarpInfoCommand(platform, warps, teleports, config));
    }

    @Override
    public void onReload(ModuleContext context) {
        warps.reload().join();
    }

}
