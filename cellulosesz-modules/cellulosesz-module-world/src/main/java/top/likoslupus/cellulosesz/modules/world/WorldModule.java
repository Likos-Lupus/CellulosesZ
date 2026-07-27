package top.likoslupus.cellulosesz.modules.world;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.BackupService;
import top.likoslupus.cellulosesz.api.world.EntityRemoveService;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.modules.world.command.*;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;
import top.likoslupus.cellulosesz.modules.world.service.DefaultBackupService;
import top.likoslupus.cellulosesz.modules.world.service.DefaultEntityRemoveService;
import top.likoslupus.cellulosesz.modules.world.service.DefaultWorldService;

import java.util.Map;

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
        config = context.configs().register("module.world", WorldConfig.class, "modules/world.yml", WorldConfig::new);
        requireNonNull(config, "WorldConfig has not been initialized").validate();
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        worlds = new DefaultWorldService(platform);
        remover = new DefaultEntityRemoveService(platform);
        backups = new DefaultBackupService(platform, context.dataDirectory()
                .getParent(), requireNonNull(config, "WorldConfig has not been initialized"));
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

        var worldOperations = context.services().require(WorldPlatformService.class);
        var entityOperations = context.services().require(EntityPlatformService.class);
        var loadedConfig = requireNonNull(config, "WorldConfig has not been initialized");
        context.commands().register(new BreakCommand(platform, worldOperations, loadedConfig));
        context.commands().register(new GcCommand(worldOperations));
        context.commands().register(new TreeCommand(platform, worldOperations, loadedConfig));
        context.commands().register(new BigTreeCommand(platform, worldOperations, loadedConfig));
        context.commands().register(new ThunderCommand(platform, worldOperations, loadedConfig));
        context.commands().register(new LightningCommand(platform, worldOperations, loadedConfig));
        context.commands().register(new FireballCommand(platform, entityOperations, loadedConfig));
        context.commands().register(new SpawnerCommand(platform, worldOperations, entityOperations, loadedConfig));
        context.commands().register(new SpawnMobCommand(platform, entityOperations, loadedConfig));
        context.commands().register(new AntiochCommand(platform, entityOperations, loadedConfig));
        context.commands().register(new BeezookaCommand(platform, entityOperations, loadedConfig));
        context.commands().register(new KittyCannonCommand(platform, entityOperations, loadedConfig));
        context.commands().register(new NukeCommand(platform, entityOperations, loadedConfig));
        registerCommandPermissions(context.services().require(PermissionCatalog.class));
    }

    @Override
    public void onReload(ModuleContext context) {
        var candidate = context.configs().require("module.world", WorldConfig.class);
        candidate.validate();
        var current = requireNonNull(config, "WorldConfig has not been initialized");
        current.copyFrom(candidate);
        requireNonNull(backups, "BackupService has not been initialized").configure(current);
    }

    private static void registerCommandPermissions(PermissionCatalog catalog) {
        Map.ofEntries(Map.entry("cellulosesz.command.break.unbreakable", "Break normally unbreakable blocks"), Map.entry("cellulosesz.command.lightning.others", "Strike lightning at another player"), Map.entry("cellulosesz.command.spawnmob.others", "Spawn mobs near another player"), Map.entry("cellulosesz.command.spawnmob.entity.*", "Spawn all permitted living entity types"), Map.entry("cellulosesz.command.spawner.delay", "Change a spawner delay"), Map.entry("cellulosesz.command.spawner.entity.*", "Configure all permitted spawner entity types"), Map.entry("cellulosesz.command.fireball.projectile.fireball", "Launch a standard fireball"), Map.entry("cellulosesz.command.fireball.projectile.small", "Launch a small fireball"), Map.entry("cellulosesz.command.fireball.projectile.large", "Launch a large fireball"), Map.entry("cellulosesz.command.fireball.projectile.arrow", "Launch an arrow"), Map.entry("cellulosesz.command.fireball.projectile.skull", "Launch a wither skull"), Map.entry("cellulosesz.command.fireball.projectile.egg", "Launch an egg"), Map.entry("cellulosesz.command.fireball.projectile.snowball", "Launch a snowball"), Map.entry("cellulosesz.command.fireball.projectile.expbottle", "Launch an experience bottle"), Map.entry("cellulosesz.command.fireball.projectile.dragon", "Launch dragon fire"), Map.entry("cellulosesz.command.fireball.projectile.splashpotion", "Launch a splash potion"), Map.entry("cellulosesz.command.fireball.projectile.lingeringpotion", "Launch a lingering potion"), Map.entry("cellulosesz.command.fireball.projectile.trident", "Launch a trident"))
                .forEach(catalog::register);
    }

}
