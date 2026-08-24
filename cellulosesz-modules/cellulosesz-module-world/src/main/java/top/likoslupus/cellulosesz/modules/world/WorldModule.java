package top.likoslupus.cellulosesz.modules.world;

import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.common.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.common.world.*;
import top.likoslupus.cellulosesz.core.command.service.PermissionCatalog;
import top.likoslupus.cellulosesz.core.module.*;
import top.likoslupus.cellulosesz.modules.world.command.*;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;
import top.likoslupus.cellulosesz.modules.world.service.BackupService;
import top.likoslupus.cellulosesz.modules.world.service.DefaultWorldService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class WorldModule implements CellulosesZModule {

    private @Nullable WorldConfig config;
    private @Nullable WorldRuntimeSettings runtimeSettings;
    private @Nullable WorldService worlds;
    private @Nullable BackupService backups;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.world",
                WorldConfig.class,
                "modules/world.yml",
                WorldConfig::new
        );
        var initial = context.configs().require("module.world", WorldConfig.class);
        initial.validate();
        config = copy(initial);
        runtimeSettings = new WorldRuntimeSettings(config);
    }

    @Override
    public void registerServices(ModuleContext context) {
        var current = requireNonNull(config, "WorldConfig has not been initialized");
        worlds = new DefaultWorldService(
                context.services().require(WorldStatePlatformService.class)
        );
        backups = new BackupService(
                context.services().require(BackupPlatformService.class),
                context.dataDirectory().getParent(),
                current
        );

        context.services().register(WorldService.class, requireNonNull(worlds));
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var directory = context.services().require(WorldDirectory.class);
        var locations = context.services().require(PlayerLocationPlatformService.class);
        var worldOperations = context.services().require(WorldPlatformService.class);
        var entityOperations = context.services().require(EntityPlatformService.class);
        var targeting = context.services().require(PlayerTargetingService.class);
        var removals = context.services().require(MinecraftEntityRemovalOperations.class);
        var current = requireNonNull(
                runtimeSettings,
                "WorldRuntimeSettings has not been initialized"
        );

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
                        locations,
                        current
                )
        );

        registerCommandPermissions(context.services().require(PermissionCatalog.class));
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var previous = requireNonNull(config, "WorldConfig has not been initialized");
        var loaded = reload.configs().require("module.world", WorldConfig.class);
        loaded.validate();
        var candidate = copy(loaded);
        var backupService = requireNonNull(backups, "BackupService has not been initialized");
        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    backupService.configure(candidate);
                    requireNonNull(
                            runtimeSettings,
                            "WorldRuntimeSettings has not been initialized"
                    ).configure(candidate);
                    config = candidate;
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    backupService.configure(previous);
                    requireNonNull(
                            runtimeSettings,
                            "WorldRuntimeSettings has not been initialized"
                    ).configure(previous);
                    config = previous;
                    return CompletableFuture.completedFuture(null);
                }
        ));
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

    private static WorldConfig copy(WorldConfig source) {
        var copy = new WorldConfig();
        copy.copyFrom(source);
        return copy;
    }

}
