package top.likoslupus.cellulosesz.modules.world;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.BackupService;
import top.likoslupus.cellulosesz.api.world.EntityRemoveService;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.modules.world.command.BackupCommand;
import top.likoslupus.cellulosesz.modules.world.command.RemoveCommand;
import top.likoslupus.cellulosesz.modules.world.command.TimeCommand;
import top.likoslupus.cellulosesz.modules.world.command.WeatherCommand;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;
import top.likoslupus.cellulosesz.modules.world.service.DefaultBackupService;
import top.likoslupus.cellulosesz.modules.world.service.DefaultEntityRemoveService;
import top.likoslupus.cellulosesz.modules.world.service.DefaultWorldService;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "world",
        name = "World",
        description = "World time, weather, and entity cleanup commands.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
public final class WorldModule implements CellulosesZModule {

    private @Nullable WorldConfig config;
    private @Nullable WorldService worlds;
    private @Nullable EntityRemoveService remover;
    private @Nullable DefaultBackupService backups;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.world",
                WorldConfig.class,
                "modules/world.yml",
                WorldConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        worlds = new DefaultWorldService(platform);
        remover = new DefaultEntityRemoveService(platform);
        backups = new DefaultBackupService(
                platform,
                context.dataDirectory().getParent(),
                requireNonNull(config, "WorldConfig has not been initialized")
        );
        context.services().register(WorldService.class, worlds);
        context.services().register(EntityRemoveService.class, remover);
        context.services().register(BackupService.class, backups);
        context.services().register(DefaultBackupService.class, backups);
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        context.commands().register(new TimeCommand(platform, config, worlds));
        context.commands().register(new WeatherCommand(platform, config, worlds));
        context.commands().register(new RemoveCommand(platform, config, remover));
        context.commands().register(new BackupCommand(backups));
    }

    @Override
    public void onReload(ModuleContext context) {
        config = context.configs().require("module.world", WorldConfig.class);
        requireNonNull(backups, "BackupService has not been initialized").configure(
                requireNonNull(config, "WorldConfig has not been initialized")
        );
    }

}
