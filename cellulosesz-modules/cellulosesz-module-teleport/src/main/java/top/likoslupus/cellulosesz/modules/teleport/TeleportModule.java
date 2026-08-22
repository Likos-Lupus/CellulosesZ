package top.likoslupus.cellulosesz.modules.teleport;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.event.PlayerDamageEvent;
import top.likoslupus.cellulosesz.api.event.PlayerDeathEvent;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.PlayerMoveEvent;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistry;
import top.likoslupus.cellulosesz.modules.teleport.application.*;
import top.likoslupus.cellulosesz.modules.teleport.command.*;
import top.likoslupus.cellulosesz.modules.teleport.service.*;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class TeleportModule implements CellulosesZModule {

    private @Nullable TeleportConfig config;
    private @Nullable TeleportRuntimeSettings runtimeSettings;
    private @Nullable TeleportRequestService requests;
    private @Nullable OfflineLocationService offlineLocations;
    private @Nullable RandomTeleportSettingsService randomSettings;
    private @Nullable TeleportService teleports;
    private @Nullable TaskHandle requestExpiryTask;

    @Override
    public void registerConfigs(ModuleContext context) {
        context.configs().register(
                "module.teleport",
                TeleportConfig.class,
                "modules/teleport.yml",
                TeleportConfig::new
        );
        config = context.configs().require(
                "module.teleport",
                TeleportConfig.class
        ).validatedCopy();
        runtimeSettings = new TeleportRuntimeSettings(config);
    }

    @Override
    public void registerServices(ModuleContext context) {
        var current = requireNonNull(config, "TeleportConfig has not been initialized");
        var settings = requireNonNull(
                runtimeSettings,
                "TeleportRuntimeSettings has not been initialized"
        );
        var storage = context.services().require(StorageService.class);
        var locations = context.services().require(PlayerLocationPlatformService.class);
        var operations = context.services().require(TeleportOperations.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);
        var players = context.services().require(PlayerDirectory.class);
        var resolver = context.services().require(PlayerResolver.class);
        var worlds = context.services().require(WorldDirectory.class);
        var users = context.services().require(UserService.class);
        var audience = context.services().require(PlayerAudienceService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var clock = Clock.systemUTC();
        var root = context.dataDirectory().getParent().resolve("teleport");
        var backLocations = new DefaultBackLocationService(
                locations,
                storage,
                root.resolve("back-locations.json")
        );

        teleports = new DefaultTeleportService(
                operations,
                locations,
                serverThread,
                context.scheduler(),
                backLocations,
                clock
        );
        requests = new DefaultTeleportRequestService(clock);
        offlineLocations = new JsonOfflineLocationService(
                storage,
                root.resolve("offline-locations.json")
        );
        randomSettings = new JsonRandomTeleportSettingsService(
                storage,
                root.resolve("random-settings.json"),
                new RandomTeleportSettings(
                        0.0D,
                        0.0D,
                        current.randomTeleport.minRadius,
                        current.randomTeleport.maxRadius
                )
        );

        var randomTeleports = new DefaultRandomTeleportService(
                operations,
                worlds,
                RandomGenerator.getDefault(),
                settings
        );
        var commandService = new DefaultTeleportCommandService(
                players,
                resolver,
                locations,
                operations,
                teleports,
                offlineLocations,
                worlds,
                users,
                serverThread,
                settings
        );
        var requestCommands = new DefaultTeleportRequestCommandService(
                teleports,
                requests,
                users,
                players,
                resolver,
                locations,
                audience,
                renderer,
                serverThread,
                context.services().optional(VanishService.class),
                settings
        );
        var preferenceCommands = new DefaultTeleportPreferenceCommandService(users, resolver);
        var randomCommands = new DefaultRandomTeleportCommandService(
                randomSettings,
                randomTeleports,
                teleports,
                locations,
                worlds,
                serverThread,
                settings
        );

        context.services().register(BackLocationService.class, backLocations);
        context.services().register(TeleportService.class, teleports);
        context.services().register(TeleportRequestService.class, requests);
        context.services().register(OfflineLocationService.class, offlineLocations);
        context.services().register(RandomTeleportSettingsService.class, randomSettings);
        context.services().register(RandomTeleportService.class, randomTeleports);
        context.services().register(TeleportCommandService.class, commandService);
        context.services().register(TeleportRequestCommandService.class, requestCommands);
        context.services().register(TeleportPreferenceCommandService.class, preferenceCommands);
        context.services().register(RandomTeleportCommandService.class, randomCommands);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var service = requireNonNull(
                teleports,
                "TeleportService has not been initialized"
        );
        var requestService = requireNonNull(
                requests,
                "TeleportRequestService has not been initialized"
        );
        var offline = requireNonNull(
                offlineLocations,
                "OfflineLocationService has not been initialized"
        );
        var audience = context.services().require(PlayerAudienceService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var serverThread = context.services().require(ServerThreadExecutor.class);

        context.events().listen(
                PlayerMoveEvent.class,
                event -> {
                    if (event.changedBlock()) {
                        service.cancelWarmup(
                                event.player().uuid(),
                                TeleportStatus.CANCELLED_MOVE
                        );
                    }
                }
        );
        context.events().listen(
                PlayerDamageEvent.class,
                event ->
                        service.cancelWarmup(
                                event.player().uuid(),
                                TeleportStatus.CANCELLED_DAMAGE
                        )
        );
        context.events().listen(
                PlayerDeathEvent.class,
                event -> {
                    service.cancelWarmup(
                            event.player().uuid(),
                            TeleportStatus.CANCELLED_DEATH
                    );
                    service.rememberBackLocation(
                            event.player().uuid(),
                            event.location()
                    ).whenComplete((_, failure) -> {
                        if (failure == null) {
                            return;
                        }
                        context.logger().error(
                                "Failed to persist death back location for "
                                        + event.player().uuid(),
                                failure
                        );
                        serverThread.execute(() -> audience.send(
                                event.player(),
                                renderer.render(
                                        audience.locale(event.player()),
                                        "service.teleport.back-persistence-failed",
                                        MessageArguments.empty()
                                )
                        ));
                    });
                }
        );
        context.events().listen(
                PlayerDisconnectEvent.class,
                event -> {
                    service.cancelWarmup(
                            event.player().uuid(),
                            TeleportStatus.CANCELLED_DISCONNECT
                    );
                    requestService.clearFor(event.player().uuid());
                    offline.remember(
                            event.player().uuid(),
                            event.location()
                    ).whenComplete((_, failure) -> {
                        if (failure != null) {
                            context.logger().error(
                                    "Failed to persist logout location for "
                                            + event.player().uuid(),
                                    failure
                            );
                        }
                    });
                }
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var registry = context.services().require(CommandRegistry.class);
        var players = context.services().require(PlayerDirectory.class);
        var worlds = context.services().require(WorldDirectory.class);
        var commandService = context.services().require(TeleportCommandService.class);
        var requestCommands = context.services().require(TeleportRequestCommandService.class);
        var preferenceCommands = context.services().require(TeleportPreferenceCommandService.class);
        var randomCommands = context.services().require(RandomTeleportCommandService.class);
        var requestService = requireNonNull(
                requests,
                "TeleportRequestService has not been initialized"
        );
        var current = requireNonNull(
                config,
                "TeleportConfig has not been initialized"
        );

        track(
                context,
                registry,
                "tp-command",
                new TpCommand(commandService, players)
        );
        track(
                context,
                registry,
                "tphere-command",
                new TpHereCommand(commandService, players)
        );
        track(
                context,
                registry,
                "tppos-command",
                new TpPosCommand(commandService, players, worlds)
        );
        track(
                context,
                registry,
                "tpa-command",
                new TpaCommand(requestCommands, players)
        );
        track(
                context,
                registry,
                "tpahere-command",
                new TpaHereCommand(requestCommands, players)
        );
        track(
                context,
                registry,
                "tpaccept-command",
                new TpAcceptCommand(requestCommands, requestService, players)
        );
        track(
                context,
                registry,
                "tpdeny-command",
                new TpDenyCommand(requestCommands, requestService, players)
        );
        track(
                context,
                registry,
                "tpacancel-command",
                new TpaCancelCommand(requestCommands, requestService, players)
        );
        track(
                context,
                registry,
                "tptoggle-command",
                new TpToggleCommand(preferenceCommands, players)
        );
        track(
                context,
                registry,
                "tpauto-command",
                new TpAutoCommand(preferenceCommands, players)
        );
        track(
                context,
                registry,
                "tpaall-command",
                new TpaAllCommand(requestCommands, players)
        );
        track(
                context,
                registry,
                "tpall-command",
                new TpAllCommand(commandService, players)
        );
        track(
                context,
                registry,
                "tpo-command",
                new TpoCommand(commandService, players)
        );
        track(
                context,
                registry,
                "tpohere-command",
                new TpoHereCommand(commandService, players)
        );
        track(
                context,
                registry,
                "tpoffline-command",
                new TpOfflineCommand(commandService, players)
        );
        track(
                context,
                registry,
                "settpr-command",
                new SetTprCommand(randomCommands, players, worlds)
        );
        track(
                context,
                registry,
                "back-command",
                new BackCommand(commandService, players)
        );
        track(
                context,
                registry,
                "jump-command",
                new JumpCommand(commandService, players, current.maximumJumpDistance)
        );
        track(
                context,
                registry,
                "top-command",
                new TopCommand(commandService, players)
        );
        track(
                context,
                registry,
                "bottom-command",
                new BottomCommand(commandService, players)
        );
        track(
                context,
                registry,
                "world-command",
                new WorldCommand(commandService, players, worlds)
        );
        track(
                context,
                registry,
                "tpr-command",
                new TprCommand(randomCommands, players)
        );
    }

    private static void track(
            ModuleContext context,
            CommandRegistry registry,
            String id,
            CommandContributor contributor
    ) {
        context.scope().own(registry.register(id, contributor));
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        scheduleRequestExpiry(context);
    }

    @Override
    public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext reload) {
        var context = reload.module();
        var previous = requireNonNull(config, "TeleportConfig has not been initialized");
        var candidate = reload.configs()
                .require("module.teleport", TeleportConfig.class)
                .validatedCopy();

        return CompletableFuture.completedFuture(PreparedReloads.of(
                () -> {
                    config = candidate;
                    requireNonNull(
                            runtimeSettings,
                            "TeleportRuntimeSettings has not been initialized"
                    ).configure(candidate);
                    scheduleRequestExpiry(context);
                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    config = previous;
                    requireNonNull(
                            runtimeSettings,
                            "TeleportRuntimeSettings has not been initialized"
                    ).configure(previous);
                    scheduleRequestExpiry(context);
                    return CompletableFuture.completedFuture(null);
                }
        ));
    }

    private void scheduleRequestExpiry(ModuleContext context) {
        if (requestExpiryTask != null) {
            requestExpiryTask.close();
        }
        requestExpiryTask = context.scope().own(context.scheduler().syncRepeating(
                () -> requireNonNull(
                        requests,
                        "TeleportRequestService has not been initialized"
                ).clearExpired(),
                20L,
                20L
        ));
    }

}
