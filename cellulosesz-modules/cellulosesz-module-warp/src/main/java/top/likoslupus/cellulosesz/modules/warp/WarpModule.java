package top.likoslupus.cellulosesz.modules.warp;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.warp.application.DefaultWarpCommandService;
import top.likoslupus.cellulosesz.modules.warp.application.WarpCommandService;
import top.likoslupus.cellulosesz.modules.warp.command.WarpCommand;
import top.likoslupus.cellulosesz.modules.warp.service.JsonWarpService;

import static java.util.Objects.requireNonNull;

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

    @SuppressWarnings("resource")
    @Override
    public void registerServices(ModuleContext context) {
        var storage = context.services().require(StorageService.class);
        var root = context.dataDirectory().getParent();

        requireNonNull(config, "WarpConfig has not been initialized");

        warps = new JsonWarpService(storage, root.resolve("warps"), config);

        context.services().register(
                WarpService.class,
                warps
        );
        context.services().register(
                JsonWarpService.class,
                (JsonWarpService) warps
        );
        context.services().register(
                WarpCommandService.class,
                new DefaultWarpCommandService(
                        warps,
                        context.services().require(TeleportService.class),
                        context.services().require(CooldownService.class),
                        context.services().require(PlayerResolver.class),
                        context.services().require(PlayerLocationPlatformService.class),
                        context.services().require(ServerThreadExecutor.class),
                        requireNonNull(config, "WarpConfig has not been initialized")
                )
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var service = context.services().require(WarpCommandService.class);
        context.track(registry.register("warp-commands", new WarpCommand(service)));
    }

    @Override
    public void onReload(ModuleContext context) {
        config = context.configs().require("module.warp", WarpConfig.class);
        context.services().require(WarpCommandService.class)
                .configure(requireNonNull(config, "WarpConfig has not been initialized"));
        requireNonNull(warps, "WarpService has not been initialized");
        warps.reload().whenComplete((_, failure) -> {
            if (failure != null)
                context.logger().error("Failed to reload warp data; retaining the previous snapshot", failure);
        });
    }

}
