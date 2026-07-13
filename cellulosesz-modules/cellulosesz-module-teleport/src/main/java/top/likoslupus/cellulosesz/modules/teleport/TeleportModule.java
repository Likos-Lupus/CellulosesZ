package top.likoslupus.cellulosesz.modules.teleport;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.annotation.CellulosesModule;
import top.likoslupus.cellulosesz.api.event.PlayerDamageEvent;
import top.likoslupus.cellulosesz.api.event.PlayerDeathEvent;
import top.likoslupus.cellulosesz.api.event.PlayerDisconnectEvent;
import top.likoslupus.cellulosesz.api.event.PlayerMoveEvent;
import top.likoslupus.cellulosesz.api.module.CellulosesZModule;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.module.ModulePhase;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.teleport.command.*;
import top.likoslupus.cellulosesz.modules.teleport.service.*;

import java.util.Map;

import static java.util.Objects.requireNonNull;

@CellulosesModule(
        id = "teleport",
        name = "Teleport",
        description = "Teleport, request, back and random teleport services.",
        phase = ModulePhase.FEATURE,
        requires = {"user", "command"}
)
public final class TeleportModule implements CellulosesZModule {

    private @Nullable TeleportConfig config;
    private @Nullable TeleportRequestService requests;

    @Override
    public void registerConfigs(ModuleContext context) {
        config = context.configs().register(
                "module.teleport",
                TeleportConfig.class,
                "modules/teleport.yml",
                TeleportConfig::new
        );
    }

    @Override
    public void registerServices(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var storage = context.services().require(StorageService.class);

        requireNonNull(config, "TeleportConfig has not been initialized");

        var backLocations = new DefaultBackLocationService(
                platform,
                storage,
                context.dataDirectory().getParent().resolve("teleport/back-locations.json")
        );
        var safeLocations = new DefaultSafeLocationFinder(platform);
        var teleports = new DefaultTeleportService(
                platform,
                context.scheduler(),
                backLocations,
                safeLocations
        );
        requests = new DefaultTeleportRequestService();
        var randomTeleports = new DefaultRandomTeleportService(platform, config.randomTeleport.attempts);

        context.services().register(BackLocationService.class, backLocations);
        context.services().register(SafeLocationFinder.class, safeLocations);
        context.services().register(TeleportService.class, teleports);
        context.services().register(TeleportRequestService.class, requests);
        context.services().register(RandomTeleportService.class, randomTeleports);
    }

    @Override
    public void registerEvents(ModuleContext context) {
        var teleports = context.services().require(TeleportService.class);
        var platform = context.services().require(PlatformService.class);
        var renderer = context.services().require(MessageRenderer.class);
        var locales = context.services().require(LocaleResolver.class);

        context.events().listen(PlayerMoveEvent.class, event -> {
            if (event.changedBlock()) {
                teleports.cancelWarmup(
                        event.player().uuid(),
                        "service.teleport.cancelled-move"
                );
            }
        });
        context.events().listen(PlayerDamageEvent.class, event ->
                teleports.cancelWarmup(
                        event.player().uuid(),
                        "service.teleport.cancelled-damage"
                )
        );
        context.events().listen(PlayerDeathEvent.class, event -> {
            teleports.cancelWarmup(
                    event.player().uuid(),
                    "service.teleport.cancelled-death"
            );
            try {
                teleports.rememberBackLocation(
                        event.player().uuid(),
                        event.location()
                );
            } catch (RuntimeException _) {
                platform.sendMessage(
                        event.player(),
                        renderer.render(
                                locales.locale(event.player()),
                                "service.teleport.back-persistence-failed",
                                Map.of()
                        )
                );
            }
        });
        context.events().listen(PlayerDisconnectEvent.class, event ->
                teleports.cancelWarmup(
                        event.player().uuid(),
                        "service.teleport.cancelled-disconnect"
                )
        );
    }

    @Override
    public void registerCommands(ModuleContext context) {
        var platform = context.services().require(PlatformService.class);
        var teleports = context.services().require(TeleportService.class);
        var randomTeleports = context.services().require(RandomTeleportService.class);
        var users = context.services().require(UserService.class);

        requireNonNull(requests, "TeleportRequestService has not been initialized");
        requireNonNull(config, "TeleportConfig has not been initialized");

        context.commands().register(new TpCommand(platform, teleports));
        context.commands().register(new TpHereCommand(platform, teleports));
        context.commands().register(new TpPosCommand(platform, teleports));
        context.commands().register(new TpaCommand(platform, requests, users, config.requests.timeoutSeconds, false));
        context.commands().register(new TpaCommand(platform, requests, users, config.requests.timeoutSeconds, true));
        context.commands().register(new TpAcceptCommand(platform, teleports, requests));
        context.commands().register(new TpDenyCommand(platform, requests));
        context.commands().register(new TpCancelCommand(platform, requests));
        context.commands().register(new TpToggleCommand(platform, users));
        context.commands().register(new BackCommand(platform, teleports));
        context.commands().register(new JumpCommand(platform, teleports));
        context.commands().register(new TopCommand(platform, teleports));
        context.commands().register(new BottomCommand(platform, teleports));
        context.commands().register(new WorldCommand(platform, teleports));
        context.commands().register(new TprCommand(
                platform,
                teleports,
                randomTeleports,
                config.randomTeleport.minRadius,
                config.randomTeleport.maxRadius
        ));
    }

    @Override
    public void onServerStarted(ModuleContext context) {
        requireNonNull(requests, "TeleportRequestService has not been initialized");
        context.scheduler().syncRepeating(
                () -> requests.clearExpired(),
                20L,
                20L
        );
    }

}
