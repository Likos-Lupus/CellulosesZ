package top.likoslupus.cellulosesz.modules.world;

import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.world.*;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.world.command.*;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;
import top.likoslupus.cellulosesz.modules.world.service.DefaultBackupService;
import top.likoslupus.cellulosesz.modules.world.service.DefaultEntityRemoveService;
import top.likoslupus.cellulosesz.modules.world.service.DefaultWorldService;

import java.util.Map;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "world",
        name = "World",
        description = "World time, weather, and entity cleanup commands.",
        phase = ModulePhase.FEATURE,
        requires = {"command"}
)
@SuppressWarnings("resource")
public final class WorldModule implements CellulosesZModule {

    private @Nullable WorldConfig config;
    private @Nullable WorldService worlds;
    private @Nullable EntityRemoveService remover;
    private @Nullable DefaultBackupService backups;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.world",
                WorldConfig.class,
                "modules/world.yml",
                WorldConfig::new
        );
        config = context.configs().require("module.world", WorldConfig.class);
        requireNonNull(config, "WorldConfig has not been initialized").validate();
    }

    @Override
    public void registerServices(ModuleContext context) {
        var current = requireNonNull(config, "WorldConfig has not been initialized");
        worlds = new DefaultWorldService(
                context.services().require(WorldStatePlatformService.class)
        );
        remover = new DefaultEntityRemoveService(
                context.services().require(EntityRemovalPlatformService.class)
        );
        backups = new DefaultBackupService(
                context.services().require(BackupPlatformService.class),
                context.dataDirectory().getParent(),
                current
        );

        context.services().register(WorldService.class, requireNonNull(worlds));
        context.services().register(EntityRemoveService.class, requireNonNull(remover));
        context.services().register(BackupService.class, requireNonNull(backups));
        context.services().register(DefaultBackupService.class, requireNonNull(backups));
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var directory = context.services().require(WorldDirectory.class);
        var locations = context.services().require(PlayerLocationPlatformService.class);
        var players = context.services().require(PlayerDirectory.class);
        var worldOperations = context.services().require(WorldPlatformService.class);
        var entityOperations = context.services().require(EntityPlatformService.class);
        var targeting = context.services().require(PlayerTargetingService.class);
        var removals = context.services().require(EntityRemovalPlatformService.class);
        var current = requireNonNull(config, "WorldConfig has not been initialized");

        track(
                context,
                registry,
                "time-command",
                new TimeCommand(
                        requireNonNull(worlds),
                        directory,
                        locations
                )
        );
        track(
                context,
                registry,
                "weather-command",
                new WeatherCommand(
                        requireNonNull(worlds),
                        directory,
                        locations,
                        current
                )
        );
        track(
                context,
                registry,
                "remove-command",
                new RemoveCommand(removals, current)
        );
        track(
                context,
                registry,
                "backup-command",
                new BackupCommand(requireNonNull(backups))
        );
        track(
                context,
                registry,
                "gc-command",
                new GcCommand(worldOperations)
        );
        track(
                context,
                registry,
                "break-command",
                new BreakCommand(worldOperations, current)
        );
        track(
                context,
                registry,
                "tree-command",
                new TreeCommand(worldOperations, current)
        );
        track(
                context,
                registry,
                "bigtree-command",
                new BigTreeCommand(worldOperations, current)
        );
        track(
                context,
                registry,
                "thunder-command",
                new ThunderCommand(
                        worldOperations,
                        locations,
                        current
                )
        );
        track(
                context,
                registry,
                "lightning-command",
                new LightningCommand(
                        worldOperations,
                        targeting,
                        players,
                        locations,
                        current
                )
        );
        track(
                context,
                registry,
                "fireball-command",
                new FireballCommand(entityOperations, current)
        );
        track(
                context,
                registry,
                "spawner-command",
                new SpawnerCommand(
                        worldOperations,
                        entityOperations,
                        current
                )
        );
        track(
                context,
                registry,
                "spawnmob-command",
                new SpawnMobCommand(
                        entityOperations,
                        players,
                        current
                )
        );
        track(
                context,
                registry,
                "antioch-command",
                new AntiochCommand(
                        targeting,
                        entityOperations,
                        current
                )
        );
        track(
                context,
                registry,
                "beezooka-command",
                new BeezookaCommand(entityOperations, current)
        );
        track(
                context,
                registry,
                "kittycannon-command",
                new KittyCannonCommand(
                        entityOperations,
                        current
                )
        );
        track(
                context,
                registry,
                "nuke-command",
                new NukeCommand(
                        entityOperations,
                        players,
                        locations,
                        current
                )
        );

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

    @Override
    public void onUnload(ModuleContext context) {
        clearTrackedEntities(context);
    }

    @Override
    public void onServerStopping(ModuleContext context) {
        clearTrackedEntities(context);
    }

    private void clearTrackedEntities(ModuleContext context) {
        context.services().require(EntityPlatformService.class).clearTrackedEntities();
    }

    private static void track(
            ModuleContext context,
            CommandRegistry registry,
            String id,
            CommandContributor contributor
    ) {
        context.scope().own(registry.register(id, contributor));
    }

    private static void registerCommandPermissions(PermissionCatalog catalog) {
        Map.ofEntries(
                Map.entry(
                        "cellulosesz.command.break.unbreakable",
                        "Break normally unbreakable blocks"
                ),
                Map.entry(
                        "cellulosesz.command.lightning.others",
                        "Strike lightning at another player"
                ),
                Map.entry(
                        "cellulosesz.command.spawnmob.others",
                        "Spawn mobs near another player"
                ),
                Map.entry(
                        "cellulosesz.command.spawnmob.entity.*",
                        "Spawn all permitted living entity types"
                ),
                Map.entry(
                        "cellulosesz.command.spawner.delay",
                        "Change a spawner delay"
                ),
                Map.entry(
                        "cellulosesz.command.spawner.entity.*",
                        "Configure all permitted spawner entity types"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.fireball",
                        "Launch a standard fireball"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.small",
                        "Launch a small fireball"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.large",
                        "Launch a large fireball"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.arrow",
                        "Launch an arrow"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.skull",
                        "Launch a wither skull"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.egg",
                        "Launch an egg"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.snowball",
                        "Launch a snowball"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.expbottle",
                        "Launch an experience bottle"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.dragon",
                        "Launch dragon fire"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.splashpotion",
                        "Launch a splash potion"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.lingeringpotion",
                        "Launch a lingering potion"
                ),
                Map.entry(
                        "cellulosesz.command.fireball.projectile.trident",
                        "Launch a trident"
                )
        ).forEach(catalog::register);
    }

}
